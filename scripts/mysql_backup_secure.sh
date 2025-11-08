#!/bin/bash

# MySQL Database Backup Script (Secure Version)
# This script will create a backup of your MySQL database using a secure configuration file

# Add mysql-client to PATH if not already there
export PATH="/opt/homebrew/opt/mysql-client/bin:$PATH"

# Read database connection details
read -p "Enter MySQL host [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "Enter MySQL port [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "Enter database name: " DB_NAME
read -p "Enter MySQL username: " DB_USER
read -s -p "Enter MySQL password: " DB_PASS
echo ""

# Create a temporary config file
CONFIG_FILE=$(mktemp /tmp/mysql_backup_XXXXX.cnf)
chmod 600 $CONFIG_FILE

# Write MySQL config to the temporary file
cat > $CONFIG_FILE << EOF
[client]
host=$DB_HOST
port=$DB_PORT
user=$DB_USER
password=$DB_PASS
EOF

# Get current timestamp for backup filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="./backups"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Generate and display the backup command
echo -e "\n# Backup Command:"
echo "# This command will create a full backup of the MySQL database"
echo "mysqldump --defaults-extra-file=$CONFIG_FILE $DB_NAME > $BACKUP_FILE"

# Ask for confirmation
read -p "Do you want to execute this backup command? (y/n): " confirm
if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
    echo "Starting backup..."
    
    # Execute the backup
    mysqldump --defaults-extra-file=$CONFIG_FILE $DB_NAME > $BACKUP_FILE
    
    # Check if backup was successful
    if [ $? -eq 0 ]; then
        echo -e "\n✅ Backup completed successfully!"
        echo "Backup saved to: $BACKUP_FILE"
    else
        echo -e "\n❌ Backup failed. Please check the error messages above."
        echo "Possible issues:"
        echo "1. Incorrect username or password"
        echo "2. User doesn't have sufficient privileges"
        echo "3. Database doesn't exist"
        echo "4. MySQL server is not running"
    fi
else
    echo "Backup cancelled."
fi

# Clean up the temporary config file
rm -f $CONFIG_FILE
