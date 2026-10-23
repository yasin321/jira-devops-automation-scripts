#!/bin/bash
# Linux Server Health Check & Docker Backup Script

LOG_FILE="/var/log/server_health.log"
DATE=$(date '+%Y-%m-%d %H:%M:%S')

echo "[$DATE] Starting system health check..." >> $LOG_FILE

# Check Disk Usage
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
    echo "[$DATE] WARNING: Disk usage is above 80% (${DISK_USAGE}%)" >> $LOG_FILE
fi

# Check Running Docker Containers
RUNNING_CONTAINERS=$(docker ps -q | wc -l)
echo "[$DATE] Active Docker containers: $RUNNING_CONTAINERS" >> $LOG_FILE

echo "[$DATE] Health check completed successfully." >> $LOG_FILE
