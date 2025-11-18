from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class UserBase(BaseModel):
    email: str
    name: str


class UserCreate(UserBase):
    pass


class UserResponse(UserBase):
    id: int
    created_at: datetime

    class Config:
        orm_mode = True


class OrderBase(BaseModel):
    amount: float
    description: Optional[str] = None
    status: str = "pending"


class OrderCreate(OrderBase):
    user_id: int


class OrderResponse(OrderBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True
