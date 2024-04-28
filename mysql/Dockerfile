FROM mysql:8.0

COPY ./conf.d/mysql.cnf /etc/mysql/conf.d/mysql.cnf

# Copy the SQL script to a specific directory inside the image
COPY ./sql/create-tables.sql /docker-entrypoint-initdb.d/1-create-tables.sql
COPY ./sql/insert-init-values.sql /docker-entrypoint-initdb.d/2-insert-init-values.sql