# Backend Setup

## 1. ต้องติดตั้งอะไรบ้าง

ให้ติดตั้งแค่นี้:
- Git
- Java 21
- VS Code
- VS Code Extension: `Extension Pack for Java`

หมายเหตุ:
- โปรเจกต์ backend นี้เป็น Spring Boot
- ไม่ต้องลง `npm` เพื่อรัน backend

## 2. เปิดโปรเจกต์ตรงไหน

backend อยู่ในโฟลเดอร์:

```text
expiry-system
```

หลัง clone โปรเจกต์แล้ว ให้เปิดโฟลเดอร์นี้ใน VS Code

## 3. ใช้ไฟล์ `.env`

ให้ใช้ไฟล์ `.env` แบบเดียวกันกับโปรเจกต์นี้ โดยวางไว้ที่:

```text
expiry-system/.env
```

โปรเจกต์จะอ่านไฟล์นี้อัตโนมัติ

ถ้าใช้ `.env` เดิมจากคนส่งโปรเจกต์ ก็ไม่ต้องแก้อะไร

## 4. คำสั่งรัน backend

เปิด Terminal ในโฟลเดอร์ `expiry-system` แล้วรัน:

```bash
./mvnw spring-boot:run
```

ถ้าเป็น Windows:

```bash
mvnw.cmd spring-boot:run
```

ถ้ารันสำเร็จ backend จะเปิดที่:

```text
http://localhost:8080
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## 5. ถ้าจะรันผ่านปุ่ม Run ใน VS Code

1. เปิดโฟลเดอร์ `expiry-system`
2. รอให้ Java extension โหลดโปรเจกต์
3. เปิดไฟล์ `src/main/java/com/app/expiry_system/ExpirySystemApplication.java`
4. กด `Run`

## 6. ถ้าเจอรันไม่ได้

### กรณี `./mvnw: Permission denied`

รัน:

```bash
chmod +x mvnw
```

แล้วรันใหม่:

```bash
./mvnw spring-boot:run
```

### กรณี Java ไม่ตรง

เช็กว่าเป็น Java 21:

```bash
java -version
```

## 7. เรื่อง npm

ใน repository นี้ยังไม่มี frontend project และไม่มี `package.json`

ดังนั้นตอนนี้:
- backend ไม่ต้องใช้ `npm install`
- backend ไม่ต้องใช้ `npm run dev`

ถ้ามี frontend แยกอีก repo ค่อยไปรัน `npm install` และ `npm run dev` ใน repo ของ frontend นั้น

## 8. สรุปสั้นสุด

1. ติดตั้ง Java 21
2. เปิดโฟลเดอร์ `expiry-system` ใน VS Code
3. วางไฟล์ `.env` ไว้ที่ `expiry-system/.env`
4. รัน:

```bash
./mvnw spring-boot:run
```

5. เปิด:

```text
http://localhost:8080/swagger-ui.html
```
