# คู่มือทดสอบ Ingredient API

เอกสารนี้ใช้สำหรับทดสอบ API กลุ่มวัตถุดิบของระบบ Smart Expiry Date Notification System

Base URL ตัวอย่าง:

```text
http://localhost:8080
```

ทุก API ในกลุ่ม ingredient ต้องส่ง JWT token:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

## 1. เตรียมข้อมูลก่อนทดสอบ

### 1.1 สมัครหรือเข้าสู่ระบบ

ถ้ายังไม่มี user ให้สมัคร:

```http
POST /api/auth/register
```

```json
{
  "email": "manager01@test.com",
  "password": "12345678",
  "displayName": "Manager 01",
  "role": "MANAGER"
}
```

ผลลัพธ์ที่คาดหวัง:

- status `201 Created`
- ได้ `accessToken`
- ได้ข้อมูล user

ถ้ามี user แล้วให้ login:

```http
POST /api/auth/login
```

```json
{
  "email": "manager01@test.com",
  "password": "12345678"
}
```

ผลลัพธ์ที่คาดหวัง:

- status `200 OK`
- ได้ `accessToken`
- นำ `accessToken` ไปใส่ใน header ของทุก ingredient API

### 1.2 สร้างร้านอาหาร

Ingredient API ต้องใช้ `restaurantId` ที่ตรงกับ user ที่ login อยู่

```http
POST /api/restaurants
```

```json
{
  "name": "Test Kitchen"
}
```

ผลลัพธ์ที่คาดหวัง:

- status `201 Created`
- ได้ `id` ของร้าน เช่น `restaurantId`
- เก็บค่า `id` นี้ไว้ใช้กับ ingredient API

ตัวแปรที่ใช้ในเอกสารนี้:

```text
accessToken = token จาก login/register
restaurantId = id จากการสร้างร้าน
ingredientId = id จากการเพิ่มวัตถุดิบ
```

## 2. Response ของ Ingredient

API ส่วนใหญ่จะคืนข้อมูลรูปแบบนี้:

```json
{
  "id": "ingredient-id",
  "restaurantId": "restaurant-id",
  "name": "เนื้อหมู - สันคอหมู",
  "lotId": "lot-id",
  "lotName": "เนื้อหมู",
  "category": "meat",
  "initialQuantity": 2,
  "quantity": 2,
  "unit": "kg",
  "categoryUnitHint": "kg",
  "expiryDate": "2026-08-12",
  "notifyDaysBefore": 2,
  "status": "ACTIVE",
  "daysLeft": 6,
  "expiring": false,
  "expired": false,
  "scannedBy": "user-id",
  "scannedAt": "2026-08-06T09:17:25.457Z",
  "lastUsedAt": null,
  "updatedBy": "user-id",
  "createdAt": "2026-08-06T09:17:25.457Z",
  "updatedAt": "2026-08-06T09:17:25.457Z"
}
```

หมายเหตุ:

- `lotId` และ `lotName` จะมีค่าเมื่อเพิ่มผ่าน batch lot
- ถ้าเพิ่มแบบปกติ `POST /api/ingredients` ค่า `lotId` และ `lotName` อาจเป็น `null`
- `daysLeft` คำนวณจากวันที่ปัจจุบันถึง `expiryDate`
- `expiring` เป็น `true` เมื่อยังไม่หมดอายุและเหลือวันน้อยกว่าหรือเท่ากับ `notifyDaysBefore`
- `expired` เป็น `true` เมื่อหมดอายุแล้ว

## 3. เพิ่มวัตถุดิบ 1 รายการ

### Endpoint

```http
POST /api/ingredients
```

### ใช้ทำอะไร

ใช้เพิ่มวัตถุดิบ 1 รายการ เช่น กะทิ 5 liter หรือ อกไก่ 10 kg

### Request

```json
{
  "restaurantId": "<restaurantId>",
  "name": "กะทิ",
  "category": "dairy",
  "initialQuantity": 5,
  "quantity": 5,
  "unit": "liter",
  "categoryUnitHint": "liter",
  "expiryDate": "2026-08-12",
  "notifyDaysBefore": 2,
  "scannedBy": "user-001",
  "scannedAt": "2026-08-06T09:17:25.457Z"
}
```

### ทดสอบอะไรบ้าง

- ส่งข้อมูลครบทุก field ที่จำเป็น
- `quantity` ต้องมากกว่า 0
- `initialQuantity` ต้องมากกว่าหรือเท่ากับ `quantity`
- `restaurantId` ต้องตรงกับร้านของ user ที่ login

### ผลลัพธ์ที่คาดหวัง

- status `201 Created`
- ได้ ingredient 1 รายการ
- `status` เป็น `ACTIVE` ถ้า `expiryDate` ยังไม่หมดอายุ
- `category` ถูกบันทึกเป็นตัวพิมพ์เล็ก
- มีการบันทึก usage history เป็น `ADDED`

## 4. เพิ่มวัตถุดิบหลายชิ้นส่วนใน lot เดียวกัน

### Endpoint

```http
POST /api/ingredients/batch
```

### ใช้ทำอะไร

ใช้เพิ่มวัตถุดิบหลายรายการพร้อมกันใน lot เดียว เช่น กดปุ่ม "เพิ่มเนื้อหมู" แล้วเลือกชิ้นส่วนหมูหลายรายการ เช่น สันคอหมู หมูสามชั้น สะโพกหมู โดยทุกชิ้นใช้วันที่หมดอายุ หมวดหมู่ หน่วย และจำนวนวันแจ้งเตือนชุดเดียวกัน

### Request

```json
{
  "restaurantId": "<restaurantId>",
  "lotName": "เนื้อหมู",
  "category": "meat",
  "unit": "kg",
  "categoryUnitHint": "kg",
  "expiryDate": "2026-08-12",
  "notifyDaysBefore": 2,
  "scannedBy": "user-001",
  "scannedAt": "2026-08-06T09:17:25.457Z",
  "items": [
    {
      "partName": "สันคอหมู",
      "initialQuantity": 2,
      "quantity": 2
    },
    {
      "partName": "หมูสามชั้น",
      "initialQuantity": 3,
      "quantity": 3
    },
    {
      "partName": "สะโพกหมู",
      "initialQuantity": 4,
      "quantity": 4
    }
  ]
}
```

### ทดสอบอะไรบ้าง

- ส่ง `items` มากกว่า 1 รายการ
- ทุก item ต้องมี `partName`, `initialQuantity`, `quantity`
- `quantity` ของแต่ละ item ต้องมากกว่า 0
- `initialQuantity` ของแต่ละ item ต้องมากกว่าหรือเท่ากับ `quantity`
- field ส่วนกลาง เช่น `expiryDate`, `unit`, `notifyDaysBefore` ถูกใช้กับทุก item

### ผลลัพธ์ที่คาดหวัง

- status `201 Created`
- response เป็น array ของ ingredient
- ได้ ingredient ตามจำนวน `items`
- ทุก ingredient มี `lotId` เดียวกัน
- ทุก ingredient มี `lotName` เป็น `เนื้อหมู`
- name ของแต่ละรายการถูกสร้างเป็น `<lotName> - <partName>` เช่น `เนื้อหมู - สันคอหมู`
- มีการบันทึก usage history เป็น `ADDED` แยกตาม ingredient แต่ละรายการ

## 5. ดูรายการวัตถุดิบทั้งหมดของร้าน

### Endpoint

```http
GET /api/ingredients?restaurantId=<restaurantId>
```

### ใช้ทำอะไร

ใช้ดูรายการวัตถุดิบทั้งหมดของร้าน

### Query Params

```text
restaurantId = id ของร้าน
status = ACTIVE, USED, DELETED, EXPIRED optional
category = หมวดหมู่ optional
```

### ตัวอย่าง

```http
GET /api/ingredients?restaurantId=<restaurantId>
```

กรองเฉพาะของ active:

```http
GET /api/ingredients?restaurantId=<restaurantId>&status=ACTIVE
```

กรองเฉพาะหมวด meat:

```http
GET /api/ingredients?restaurantId=<restaurantId>&category=meat
```

### ทดสอบอะไรบ้าง

- ไม่ส่ง `status` และ `category` เพื่อดูทั้งหมด
- ส่ง `status=ACTIVE`
- ส่ง `category=meat`
- ส่ง `restaurantId` ที่ไม่ตรงกับ user เพื่อทดสอบ unauthorized business rule

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- response เป็น array
- รายการถูก sort ตาม `expiryDate` แล้วตาม `name`
- ถ้ากรอง status หรือ category จะคืนเฉพาะรายการที่ตรงเงื่อนไข

## 6. ดูรายละเอียดวัตถุดิบรายชิ้น

### Endpoint

```http
GET /api/ingredients/{id}
```

### ใช้ทำอะไร

ใช้ดูข้อมูลวัตถุดิบ 1 รายการจาก `ingredientId`

### ตัวอย่าง

```http
GET /api/ingredients/<ingredientId>
```

### ทดสอบอะไรบ้าง

- ใช้ `ingredientId` ที่มีอยู่จริง
- ใช้ `ingredientId` ที่ไม่มีอยู่
- ใช้ ingredient ของร้านอื่น

### ผลลัพธ์ที่คาดหวัง

- ถ้าพบข้อมูล status `200 OK`
- ถ้าไม่พบข้อมูล status `400 Bad Request` พร้อม message `Ingredient not found`
- ถ้าเป็นร้านอื่น status `400 Bad Request` พร้อม message `Unauthorized to access this restaurant`

## 7. แก้ไขข้อมูลวัตถุดิบ

### Endpoint

```http
PUT /api/ingredients/{id}
```

### ใช้ทำอะไร

ใช้แก้ไขข้อมูลวัตถุดิบ เช่น ชื่อ หมวดหมู่ จำนวน หน่วย วันหมดอายุ หรือจำนวนวันแจ้งเตือน

### Request

```json
{
  "restaurantId": "<restaurantId>",
  "name": "กะทิกล่อง",
  "category": "dairy",
  "initialQuantity": 6,
  "quantity": 5,
  "unit": "liter",
  "categoryUnitHint": "liter",
  "expiryDate": "2026-08-15",
  "notifyDaysBefore": 3,
  "scannedBy": "user-001",
  "scannedAt": "2026-08-06T09:17:25.457Z"
}
```

### ทดสอบอะไรบ้าง

- แก้ชื่อหรือวันหมดอายุ
- แก้ `quantity` โดยยังไม่เกิน `initialQuantity`
- ส่ง `restaurantId` เดิมเท่านั้น
- ลองส่ง `quantity = 0`
- ลองส่ง `initialQuantity < quantity`

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- ข้อมูลถูก update
- `status` ถูกคำนวณใหม่จาก `expiryDate` และ `quantity`
- มีการบันทึก usage history เป็น `EDITED`
- ถ้าเปลี่ยน `restaurantId` จะ error `Ingredient restaurantId cannot be changed`

## 8. หักจำนวนวัตถุดิบที่ใช้

### Endpoint

```http
PATCH /api/ingredients/{id}/consume
```

### ใช้ทำอะไร

ใช้หักจำนวนวัตถุดิบเมื่อถูกนำไปใช้งาน เช่น ใช้หมูสามชั้นไป 0.5 kg

### Request

```json
{
  "quantity": 0.5,
  "note": "ใช้ทำเมนูผัดกะเพรา"
}
```

### ทดสอบอะไรบ้าง

- consume น้อยกว่าจำนวนคงเหลือ
- consume เท่ากับจำนวนคงเหลือ
- consume มากกว่าจำนวนคงเหลือ
- consume ingredient ที่เป็น `USED` หรือ `DELETED`

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- `quantity` ลดลงตามจำนวนที่ใช้
- `lastUsedAt` มีค่า
- ถ้าเหลือ 0 ระบบตั้ง `status` เป็น `USED`
- มีการบันทึก usage history เป็น `CONSUMED`
- ถ้า consume มากกว่าคงเหลือ จะ error `Consume quantity cannot exceed remaining quantity`

## 9. เติมจำนวนวัตถุดิบ

### Endpoint

```http
PATCH /api/ingredients/{id}/restock
```

### ใช้ทำอะไร

ใช้เพิ่มจำนวนวัตถุดิบในรายการเดิม เช่น เติมน้ำปลาเพิ่ม 2 bottle

### Request

```json
{
  "quantity": 2,
  "note": "ซื้อเพิ่มรอบเช้า"
}
```

### ทดสอบอะไรบ้าง

- restock ingredient ที่ยัง active
- restock ingredient ที่หมดอายุ
- restock ingredient ที่ถูก deleted

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- `quantity` เพิ่มขึ้น
- `status` ถูกคำนวณใหม่จาก `expiryDate` และจำนวนใหม่
- มีการบันทึก usage history เป็น `RESTOCKED`
- ถ้า ingredient เป็น `DELETED` จะ error `Deleted ingredient cannot be restocked`

## 10. ปรับจำนวนโดยตรง

### Endpoint

```http
PATCH /api/ingredients/{id}/adjust-quantity
```

### ใช้ทำอะไร

ใช้ปรับจำนวนให้ตรงกับการนับของจริง หรือแก้ข้อมูลที่กรอกผิด เหมาะสำหรับ Manager เท่านั้น

### Request

```json
{
  "quantity": 1.5,
  "reason": "นับสต็อกจริงแล้วเหลือ 1.5 kg"
}
```

### ทดสอบอะไรบ้าง

- Manager เรียกใช้งาน
- Employee เรียกใช้งาน
- ปรับ quantity เป็น 0
- ไม่ส่ง `reason`

### ผลลัพธ์ที่คาดหวัง

- ถ้าเป็น Manager status `200 OK`
- `quantity` ถูกเปลี่ยนเป็นค่าที่ส่งมาโดยตรง
- ถ้าปรับเป็น 0 ระบบตั้ง `status` เป็น `USED`
- มีการบันทึก usage history เป็น `ADJUSTED`
- ถ้าเป็น Employee จะ error `Only Managers can adjust ingredient quantity directly`


## 11. ตั้งวัตถุดิบเป็นใช้หมด

### Endpoint

```http
PATCH /api/ingredients/{id}/used
```

### ใช้ทำอะไร

ใช้ปิดรายการวัตถุดิบว่าใช้หมดแล้ว โดยระบบจะตั้ง `quantity` เป็น 0

### Request

ส่ง note หรือไม่ส่ง body ก็ได้

```json
{
  "note": "ใช้หมดจากการเตรียมวัตถุดิบ"
}
```

### ทดสอบอะไรบ้าง

- ส่งพร้อม note
- ส่งโดยไม่มี body
- mark used กับ ingredient ที่ยังมี quantity เหลือ

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- `quantity` เป็น 0
- `status` เป็น `USED`
- `lastUsedAt` มีค่า
- มีการบันทึก usage history เป็น `USED`

## 12. Soft delete วัตถุดิบ

### Endpoint

```http
PATCH /api/ingredients/{id}/delete
```

### ใช้ทำอะไร

ใช้ลบวัตถุดิบแบบ soft delete โดยไม่ลบ record ออกจากฐานข้อมูล แต่เปลี่ยน `status` เป็น `DELETED`

### Request

ส่ง note หรือไม่ส่ง body ก็ได้

```json
{
  "note": "เพิ่มผิดรายการ"
}
```

### ทดสอบอะไรบ้าง

- delete ingredient ที่มีอยู่จริง
- delete แล้วลอง restock
- delete แล้วลอง consume

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- `status` เป็น `DELETED`
- `quantity` ไม่ถูกเปลี่ยน
- มีการบันทึก usage history เป็น `DELETED`
- หลัง delete จะ consume ไม่ได้ และ restock ไม่ได้

## 13. เปลี่ยนสถานะวัตถุดิบ

### Endpoint

```http
PATCH /api/ingredients/{id}/status
```

### ใช้ทำอะไร

ใช้เปลี่ยนสถานะวัตถุดิบตาม workflow ของร้าน เช่น `ACTIVE`, `USED`, `DELETED`, `EXPIRED`

### Request

```json
{
  "status": "EXPIRED",
  "note": "ตรวจพบว่าหมดอายุแล้ว"
}
```

### ทดสอบอะไรบ้าง

- เปลี่ยนเป็น `EXPIRED`
- เปลี่ยนเป็น `USED`
- เปลี่ยนกลับเป็น `ACTIVE` ในกรณี quantity มากกว่า 0
- เปลี่ยนเป็น `ACTIVE` ตอน quantity เป็น 0

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- `status` ถูกเปลี่ยนตาม request
- ถ้าเปลี่ยนเป็น `USED` ระบบตั้ง `quantity` เป็น 0 และตั้ง `lastUsedAt`
- มีการบันทึก usage history เป็น `EDITED`
- ถ้าเปลี่ยนเป็น `ACTIVE` ตอน quantity เป็น 0 จะ error `Active ingredient must have quantity greater than 0`

## 14. ดูวัตถุดิบใกล้หมดอายุ

### Endpoint

```http
GET /api/ingredients/expiring?restaurantId=<restaurantId>
```

### ใช้ทำอะไร

ใช้ดูวัตถุดิบที่ยัง active และใกล้หมดอายุตาม `notifyDaysBefore`

### วิธีเตรียมข้อมูลทดสอบ

เพิ่มวัตถุดิบที่ `expiryDate` อยู่ภายในช่วง `notifyDaysBefore`

ตัวอย่าง ถ้าวันนี้คือ `2026-08-06`:

```json
{
  "restaurantId": "<restaurantId>",
  "name": "เต้าหู้",
  "category": "fresh",
  "initialQuantity": 10,
  "quantity": 10,
  "unit": "piece",
  "expiryDate": "2026-08-08",
  "notifyDaysBefore": 2
}
```

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- response เป็น array
- คืนเฉพาะ ingredient ที่ `status` เป็น `ACTIVE`
- `expiring` เป็น `true`
- เรียงตาม `expiryDate`

## 15. ดูวัตถุดิบหมดอายุ

### Endpoint

```http
GET /api/ingredients/expired?restaurantId=<restaurantId>
```

### ใช้ทำอะไร

ใช้ดูวัตถุดิบที่หมดอายุแล้ว

### วิธีเตรียมข้อมูลทดสอบ

เพิ่มวัตถุดิบที่ `expiryDate` เป็นอดีต หรือเปลี่ยน status เป็น `EXPIRED`

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- response เป็น array
- คืนรายการที่หมดอายุจาก status `EXPIRED` หรือ `expiryDate` ก่อนวันปัจจุบัน
- `expired` เป็น `true`
- เรียงตาม `expiryDate`

## 16. ดูวัตถุดิบสต็อกต่ำ

### Endpoint

```http
GET /api/ingredients/low-stock?restaurantId=<restaurantId>
```

หรือกรองหมวดหมู่:

```http
GET /api/ingredients/low-stock?restaurantId=<restaurantId>&category=meat
```

### ใช้ทำอะไร

ใช้ดูวัตถุดิบที่จำนวนเหลือน้อยกว่าหรือเท่ากับ 20% ของจำนวนตั้งต้น

### วิธีเตรียมข้อมูลทดสอบ

เพิ่มวัตถุดิบ:

```json
{
  "restaurantId": "<restaurantId>",
  "name": "น้ำปลา",
  "category": "sauce",
  "initialQuantity": 10,
  "quantity": 10,
  "unit": "bottle",
  "expiryDate": "2026-09-01",
  "notifyDaysBefore": 7
}
```

จากนั้น consume ให้เหลือ 2 หรือน้อยกว่า:

```json
{
  "quantity": 8,
  "note": "ใช้ระหว่างวัน"
}
```

### ผลลัพธ์ที่คาดหวัง

- status `200 OK`
- response เป็น array
- คืนรายการที่ `quantity <= initialQuantity * 0.20`
- ไม่คืนรายการที่ `status` เป็น `DELETED`
- ถ้าส่ง `category` จะคืนเฉพาะหมวดหมู่นั้น
- เรียงตาม `quantity` จากน้อยไปมาก แล้วตาม `name`

## 17. Error response ที่พบบ่อย

รูปแบบ error:

```json
{
  "timestamp": "2026-08-06T09:17:25.457Z",
  "status": 400,
  "error": "Bad Request",
  "message": "ข้อความ error"
}
```

กรณีที่ควรทดสอบ:

| กรณี | วิธีทดสอบ | ผลลัพธ์ที่คาดหวัง |
|---|---|---|
| ไม่ส่ง token | เรียก ingredient API โดยไม่มี Authorization header | `401 Unauthorized` |
| restaurantId ว่าง | ส่ง `restaurantId` เป็นค่าว่าง | `400 Bad Request` |
| user ไม่มี restaurant | ใช้ user ที่ยังไม่ join/create restaurant | `400 Bad Request` |
| restaurantId ไม่ตรงกับ user | ส่ง restaurantId ของร้านอื่น | `400 Bad Request` |
| quantity ตอน create เป็น 0 | ส่ง `quantity: 0` | `400 Bad Request` |
| initialQuantity น้อยกว่า quantity | ส่ง `initialQuantity: 1`, `quantity: 2` | `400 Bad Request` |
| consume เกินจำนวนคงเหลือ | ส่ง quantity มากกว่าที่เหลือ | `400 Bad Request` |
| Employee ปรับ adjust-quantity | login เป็น Employee แล้วเรียก endpoint | `400 Bad Request` |
| เปลี่ยนเป็น ACTIVE ตอน quantity เป็น 0 | mark used ก่อน แล้ว patch status ACTIVE | `400 Bad Request` |

## 18. ลำดับทดสอบแนะนำ

1. Register หรือ login
2. Create restaurant
3. Create ingredient แบบเดี่ยว
4. Get ingredients
5. Get ingredient by id
6. Update ingredient
7. Consume ingredient
8. Restock ingredient
9. Adjust quantity ด้วย Manager
10. Mark used
11. Soft delete
12. Create batch lot
13. Get expiring
14. Get expired
15. Get low-stock

## 19. แนวทางทดสอบ batch lot สำหรับหน้าจอเพิ่มเนื้อหมู

Flow ที่ควรทดสอบจาก frontend:

1. ผู้ใช้กดปุ่ม "เพิ่มเนื้อหมู"
2. หน้าเพิ่มข้อมูลแสดง field ส่วนกลาง:
   - วันที่หมดอายุ
   - จำนวนวันแจ้งเตือน
   - หมวดหมู่
   - หน่วย
3. มี dropdown สำหรับเลือกชิ้นส่วนหมู เช่น:
   - สันคอหมู
   - หมูสามชั้น
   - สะโพกหมู
   - ซี่โครงหมู
   - หมูบด
4. ผู้ใช้กดปุ่มบวกเพื่อเพิ่มชิ้นส่วนหลายรายการ
5. กรอก quantity ของแต่ละชิ้นส่วน
6. กดบันทึกครั้งเดียว
7. frontend ส่ง `POST /api/ingredients/batch`
8. ระบบบันทึกหลาย ingredient ใน `lotId` เดียวกัน

ผลลัพธ์ที่ต้องตรวจ:

- จำนวน ingredient ที่ได้เท่ากับจำนวนชิ้นส่วนที่เลือก
- ทุก ingredient มี `lotId` เดียวกัน
- ทุก ingredient มีวันหมดอายุเดียวกัน
- ทุก ingredient ยัง consume/restock แยกกันได้
- หน้า list สามารถแสดงแยกเป็นแต่ละชิ้นส่วน หรือ group ตาม `lotName` ได้
