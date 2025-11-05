from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class UserBase(BaseModel):
    email: str  # Issue: should be EmailStr for validation
    name: str


class UserCreate(UserBase):
    # Issue: missing password field if we need authentication
    pass


class UserResponse(UserBase):
    id: int
    created_at: datetime

    class Config:
        orm_mode = True  # Issue: should be from_attributes=True (Pydantic v2)


class OrderBase(BaseModel):
    amount: float  # Issue: should validate > 0
    description: Optional[str] = None
    status: str = "pending"  # Issue: should validate against allowed values


class OrderCreate(OrderBase):
    user_id: int
    # Issue: missing validation that amount is positive
    # Issue: missing validation that status is valid


class OrderResponse(OrderBase):
    id: int
    user_id: int
    created_at: datetime

    class Config:
        orm_mode = True  # Issue: should be from_attributes=True (Pydantic v2)


# Issue: missing update schemas (UserUpdate, OrderUpdate)
# Issue: missing proper error messages in validators
