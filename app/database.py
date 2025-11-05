from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from app.models import Base
import os

# Issue: hardcoded database URL, should use environment variables properly
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./test.db")

# Issue: no connection pooling configuration
engine = create_engine(
    DATABASE_URL,
    connect_args={"check_same_thread": False} if "sqlite" in DATABASE_URL else {},
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def init_db():
    """Initialize database tables"""
    # Issue: this should be done via Alembic migrations, not here
    Base.metadata.create_all(bind=engine)


def get_db():
    """Dependency for getting database session"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
        # Issue: missing proper error handling and rollback on exceptions


# Issue: no connection retry logic
# Issue: no connection health check
# Issue: init_db() called at module level would be better as startup event
