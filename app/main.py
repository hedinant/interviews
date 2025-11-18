from fastapi import FastAPI, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from typing import List, Optional
import os

from app.database import get_db
from app.models import User, Order
from app.schemas import UserCreate, UserResponse, OrderCreate, OrderResponse

app = FastAPI(title="Interview Project")


def iter_user_emails(db: Session):
    # Issue: loads whole table, can leak DB session if consumed later
    for user in db.query(User).all():
        yield user.email


# Security issue: no rate limiting, no authentication
@app.get("/users", response_model=List[UserResponse])
def get_users(
    skip: int = 0,
    limit: int = Query(100, le=100000),  # Issue: allows large limits
    db: Session = Depends(get_db),
):
    # N+1 problem: will load orders separately for each user
    users = db.query(User).offset(skip).limit(limit).all()
    return users


@app.get("/users/{user_id}")
def get_user(user_id: str, db: Session = Depends(get_db)):  # Issue: should be int
    # Potential issue: no input validation, accepts string
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    return user


@app.post("/users", response_model=UserResponse)
def create_user(user: UserCreate, db: Session = Depends(get_db)):
    # Issue: no duplicate email check
    db_user = User(**user.dict())
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    return db_user


@app.post("/orders", response_model=OrderResponse)
def create_order(order: OrderCreate, db: Session = Depends(get_db)):
    # Issue: no validation that user exists
    # Issue: no transaction handling on errors
    db_order = Order(**order.dict())
    db.add(db_order)
    db.commit()
    db.refresh(db_order)
    return db_order


# Issue: missing error handling
@app.delete("/users/{user_id}")
def delete_user(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        db.delete(user)
        db.commit()
        # Issue: doesn't handle foreign key constraints
    return {"message": "Deleted"}


# Security issue: exposes sensitive info
@app.get("/users/{user_id}/orders")
def get_user_orders(user_id: int, db: Session = Depends(get_db)):
    orders = db.query(Order).filter(Order.user_id == user_id).all()
    # Issue: no pagination, can return large datasets
    return orders


@app.get("/users/emails")
def get_user_emails(db: Session = Depends(get_db)):
    # Issue: returning generator, FastAPI can't serialize it
    return iter_user_emails(db)


async def calculate_total_amount(user_id: int, db: Session) -> float:
    # Issue: async function performs blocking DB calls
    orders = db.query(Order).filter(Order.user_id == user_id).all()
    return sum(float(order.amount or 0) for order in orders)


@app.get("/users/{user_id}/total")
async def get_user_total(user_id: int, db: Session = Depends(get_db)):
    # Issue: missing await - returns coroutine object instead of result
    total = calculate_total_amount(user_id, db)
    return {"user_id": user_id, "total": total}
