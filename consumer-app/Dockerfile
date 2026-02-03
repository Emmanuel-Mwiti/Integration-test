FROM python:3.10-slim
WORKDIR /app

# Install system dependencies for building Python packages + git
RUN apt-get update && apt-get install -y \
    build-essential \
    python3-dev \
    libssl-dev \
    git \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip
RUN python -m pip install --upgrade pip

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY app.py .

# Start the consumer
CMD ["python", "app.py"]