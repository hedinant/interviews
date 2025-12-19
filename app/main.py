from fastapi import FastAPI, Depends, HTTPException, Query, WebSocket
from sqlalchemy.orm import Session
from typing import List, Optional, Set
import os

from app.database import get_db
from app.models import User, Order
from app.schemas import UserCreate, UserResponse, OrderCreate, OrderResponse

app = FastAPI(title="Interview Project")

active_connections: Set[WebSocket] = set()


def iter_user_emails(db: Session):
    for user in db.query(User).yield_per(10):
        yield user.email


def iter_orders_stream(user_id: int, db: Session):
    log_file = open(f"orders_{user_id}.log", "w")
    for order in db.query(Order).filter(Order.user_id == user_id).yield_per(5):
        log_file.write(f"Order {order.id}\n")
        yield order
    log_file.close()


@app.get("/users", response_model=List[UserResponse])
def get_users(
    skip: int = 0,
    limit: int = Query(100, le=100000),
    db: Session = Depends(get_db),
):
    users = db.query(User).offset(skip).limit(limit).all()
    return users


@app.get("/users/{user_id}")
def get_user(user_id: str, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user


@app.post("/users", response_model=UserResponse)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    db_user = User(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user


@app.post("/orders", response_model=OrderResponse)
def create_order(order: OrderCreate, db: Session = Depends(get_db)):
    order_data = order.dict()
    if order_data.get("tags"):
        order_data["tags"] = ",".join(order_data["tags"])
    db_order = Order(**order_data)
    db.add(db_order)
    db.commit()
    db.refresh(db_order)
    return db_order


@app.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        db.delete(user)
        db.commit()
    return {"message": "Deleted"}


@app.get("/users/{user_id}/orders")
def get_user_orders(user_id: int, db: Session = Depends(get_db)):
    orders = db.query(Order).filter(Order.user_id == user_id).all()
    return orders


@app.get("/orders/by-tag/{tag}")
def get_orders_by_tag(tag: str, db: Session = Depends(get_db)):
    all_orders = db.query(Order).filter(Order.tags.isnot(None)).all()
    matching_orders = [o for o in all_orders if tag in (o.tags or "")]
    return matching_orders


@app.get("/users/emails")
def get_user_emails(db: Session = Depends(get_db)):
    return iter_user_emails(db)


@app.get("/users/{user_id}/orders/stream")
def stream_user_orders(user_id: int, db: Session = Depends(get_db)):
    return iter_orders_stream(user_id, db)


async def calculate_total_amount(user_id: int, db: Session) -> float:
    orders = db.query(Order).filter(Order.user_id == user_id).all()
    return sum(float(order.amount or 0) for order in orders)


@app.get("/users/{user_id}/total")
async def get_user_total(user_id: int, db: Session = Depends(get_db)):
    total = calculate_total_amount(user_id, db)
    return {"user_id": user_id, "total": total}


@app.websocket("/ws/orders/{user_id}")
async def websocket_orders(websocket: WebSocket, user_id: int):
    await websocket.accept()
    active_connections.add(websocket)
    db = next(get_db())
    while True:
        data = await websocket.receive_text()
        import json

        request_data = json.loads(data)
        status_filter = request_data.get("status")
        query = db.query(Order).filter(Order.user_id == user_id)
        if status_filter:
            query = query.filter(Order.status == status_filter)
        orders = query.all()
        await websocket.send_json([{"id": o.id, "amount": o.amount} for o in orders])
