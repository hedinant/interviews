from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from datetime import datetime

Base = declarative_base()


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True)  # Issue: should have nullable=False
    name = Column(String(100))
    created_at = Column(
        DateTime, default=datetime.utcnow
    )  # Issue: should use datetime.utcnow() or func.now()

    # Issue: no cascade delete specified
    orders = relationship("Order", back_populates="user")

    # Issue: missing __repr__ method


class Order(Base):
    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))  # Issue: missing nullable=False
    amount = Column(Float)  # Issue: should be Decimal for money
    description = Column(Text)  # Issue: no length limit, can be huge
    status = Column(
        String(50)
    )  # Issue: should be enum or at least have check constraint
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="orders")

    # Issue: missing index on user_id for faster queries
    # Issue: missing index on status if we query by status often
    # Issue: missing validation constraints
