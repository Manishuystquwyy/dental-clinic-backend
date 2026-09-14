# Oracle Cloud Always Free MySQL Setup

This project already uses MySQL through Spring Boot/JPA. Oracle Cloud's Always Free database option for this app is **MySQL HeatWave Always Free**, so no code-level database migration is needed.

## 1. Create the OCI MySQL DB system

1. In Oracle Cloud Console, open **Databases > MySQL HeatWave > DB systems**.
2. Choose **Create DB system**.
3. Select **Always Free**.
4. Create it in your home region and select/create a VCN.
5. Keep the default MySQL port `3306`.
6. Save the admin username and password somewhere secure.
7. After provisioning, copy the DB system's private endpoint IP address. For your current DB system, this is `10.0.0.96`.

Oracle MySQL HeatWave DB systems are normally reached through a private endpoint inside your VCN. That means your backend should run in the same VCN, or you should connect from your laptop using a bastion/SSH tunnel.

## 2. Allow network access

Add an ingress rule on the DB system's network security group or subnet security list:

- Source: your backend subnet CIDR, compute instance private IP, or bastion subnet CIDR
- Destination port: `3306`
- Protocol: TCP

Avoid opening MySQL to `0.0.0.0/0`.

## 3. Create the application database/user

Connect from an OCI Compute instance, Cloud Shell with private access, or a bastion tunnel:

```bash
mysql -h 10.0.0.96 -P 3306 -u <admin-user> -p
```

Then run:

```sql
CREATE DATABASE IF NOT EXISTS dental_clinic
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'dental_app'@'%' IDENTIFIED BY 'change-this-password';
GRANT ALL PRIVILEGES ON dental_clinic.* TO 'dental_app'@'%';
FLUSH PRIVILEGES;
```

The same SQL is available in `db/oci-mysql-init.sql`.

## 4. Run the app against Oracle Cloud MySQL

Copy `.env.oracle.example` values into your deployment environment, then set:

```bash
export SPRING_PROFILES_ACTIVE=oci
export SPRING_DATASOURCE_URL='jdbc:mysql://10.0.0.96:3306/dental_clinic?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='dental_app'
export SPRING_DATASOURCE_PASSWORD='<dental_app-password>'
```

Start locally:

```bash
./mvnw spring-boot:run
```

For a packaged jar:

```bash
java -jar target/com.gayatri.dentalclinic-0.0.1-SNAPSHOT.jar
```

## 5. Local laptop access through a tunnel

If your laptop cannot reach the private MySQL endpoint directly, create an SSH tunnel through an OCI Compute instance in the same VCN:

```bash
ssh -i <ssh-key-path> -L 3307:10.0.0.96:3306 opc@<compute-public-ip>
```

Then point Spring Boot to the tunnel:

```bash
export SPRING_PROFILES_ACTIVE=oci
export SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3307/dental_clinic?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export SPRING_DATASOURCE_USERNAME='dental_app'
export SPRING_DATASOURCE_PASSWORD='<dental_app-password>'
./mvnw spring-boot:run
```

## 6. Kubernetes secret example

If this backend is deployed to Kubernetes, create/update the datasource secret like this:

```bash
kubectl create secret generic dental-clinic-backend-secrets \
  --from-literal=SPRING_PROFILES_ACTIVE='oci' \
  --from-literal=SPRING_DATASOURCE_URL='jdbc:mysql://10.0.0.96:3306/dental_clinic?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC' \
  --from-literal=SPRING_DATASOURCE_USERNAME='dental_app' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<dental_app-password>' \
  --from-literal=APP_JWT_SECRET='<long-random-secret>' \
  --from-literal=APP_ADMIN_EMAIL='admin@gmail.com' \
  --from-literal=APP_ADMIN_PASSWORD='<strong-admin-password>'
```

Add the mail and Razorpay variables from `.env.oracle.example` as needed.

## Notes for production

- Keep `SPRING_JPA_HIBERNATE_DDL_AUTO=update` only while bootstrapping. For safer production changes, move to migrations with Flyway or Liquibase.
- Rotate any secrets that were previously committed to `application.properties`.
- Use the app-specific `dental_app` user instead of the MySQL admin user.
