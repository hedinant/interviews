from fastapi import FastAPI, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
import os

from app.database import get_db
from app.models import User, Order
from app.schemas import UserCreate, UserResponse, OrderCreate, OrderResponse

app = FastAPI(title="Interview Project")


def iter_user_emails(db: Session):
    for user in db.query(User).all():
        yield user.email


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
    db_order = Order(**order.dict())
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


@app.get("/users/emails")
def get_user_emails(db: Session = Depends(get_db)):
    return iter_user_emails(db)


async def calculate_total_amount(user_id: int, db: Session) -> float:
    orders = db.query(Order).filter(Order.user_id == user_id).all()
    return sum(float(order.amount or 0) for order in orders)


@app.get("/users/{user_id}/total")
async def get_user_total(user_id: int, db: Session = Depends(get_db)):
    total = calculate_total_amount(user_id, db)
    return {"user_id": user_id, "total": total}
