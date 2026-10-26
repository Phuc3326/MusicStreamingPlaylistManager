# SoundWave — VPS Deployment and Operations Guide

Huong dan toan dien trien khai va van hanh he thong SoundWave tren VPS Ubuntu 22.04 LTS su dung Docker Compose, Nginx Reverse Proxy va SSL Certbot (Let's Encrypt).

---

## 1. Thong so he thong va Ha tang

* VPS Provider: Vultr (Ubuntu 22.04 LTS x86_64)
* Server IP: 45.77.32.161
* Ten mien (Domain): https://soundwave.hufudev.me
* Web Application Container: soundwave-webapp (Port noi bo 8080 -> Port Host 8081)
* Database Container: soundwave-postgres (Port noi bo 5432 -> Port Host 5436)
* Web Server & SSL: Nginx Reverse Proxy + Let's Encrypt SSL Certbot

---

## 2. Quy trinh Trien khai tung buoc (Step-by-Step)

### Buoc 1: Cau hinh DNS (Tren Namecheap)
Tao ban ghi A trỏ domain ve IP VPS:
* Type: A Record
* Host: soundwave
* Value: 45.77.32.161
* TTL: Automatic

---

### Buoc 2: Ket noi SSH vao VPS
Tu may ca nhan, mo terminal va ket noi:
```bash
ssh root@45.77.32.161
```

---

### Buoc 3: Tai ma nguon tu GitHub ve VPS
```bash
cd ~
git clone https://github.com/Phuc3326/MusicStreamingPlaylistManager.git
cd MusicStreamingPlaylistManager
```

Neu ban da clone tu truoc va can cap nhat code moi nhat:
```bash
cd ~/MusicStreamingPlaylistManager
git pull
```

---

### Buoc 4: Khoi tao file cau hinh bien moi truong (.env)
Tao file `.env` tai thu muc goc du an tren VPS:
```bash
cat << 'EOF' > .env
# Database Configuration
POSTGRES_DB=<ten_db>
DB_USER=<ten_user>
DB_PASS=<password>

# SoundWave Administrator Credentials (Auto-seeded & Synced on Startup)
ADMIN_USERNAME=<admin_username>
ADMIN_PASSWORD=<admin_password>

# Cloudinary CDN Configuration (De trong neu dung luu tru cuc bo)
CLOUDINARY_URL=
EOF
```

Luu y: Thay doi `DB_PASS` va `ADMIN_PASSWORD` thanh mat khau bao mat cua ban.

---

### Buoc 5: Khoi dong ung dung bang Docker Compose
```bash
docker compose up -d --build
```

Kiem tra trang thai hoat dong cua cac container:
```bash
docker ps
```
Dam bao 2 container sau dang o trang thai `Up`:
* `soundwave-webapp` (0.0.0.0:8081->8080/tcp)
* `soundwave-postgres` (0.0.0.0:5436->5432/tcp)

---

### Buoc 6: Cau hinh Nginx Reverse Proxy
Tao file cau hinh Nginx moi cho SoundWave:
```bash
nano /etc/nginx/sites-available/soundwave.hufudev.me
```

Dan toan bo noi dung sau vao file:
```nginx
server {
    listen 80;
    server_name soundwave.hufudev.me;

    # Cho phep upload audio/anh dung luong toi da 30MB
    client_max_body_size 30M;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
Luu file: Bam `Ctrl + O` -> `Enter` -> `Ctrl + X`.

Kich hoat cau hinh va kiem tra cu phap:
```bash
ln -sf /etc/nginx/sites-available/soundwave.hufudev.me /etc/nginx/sites-enabled/
nginx -t
systemctl reload nginx
```

---

### Buoc 7: Cai dat chung chi SSL HTTPS (Certbot)
Chay lenh sau de Certbot tu dong cap chung chi SSL Let's Encrypt:
```bash
certbot --nginx -d soundwave.hufudev.me
```

Chon tuy chon tu dong chuyen huong HTTP sang HTTPS khi duoc hoi. Sau khi hoan tat, truy cap:
https://soundwave.hufudev.me

---

## 3. Cheatsheet Van hanh va Bao tri (Operations)

### Xem Logs thoi gian thuc
```bash
# Xem log webapp
docker logs -f --tail 100 soundwave-webapp

# Xem log database postgres
docker logs -f --tail 100 soundwave-postgres
```

### Cap nhat Code moi tu GitHub
```bash
cd ~/MusicStreamingPlaylistManager
git pull
docker compose up -d --build music-app
```

### Thay doi Bien moi truong (.env)
Sau khi sua file `.env` bang `nano .env`:
```bash
docker compose up -d
```
*(Khong can flag `--build`, Docker Compose se tu dong recreate container de cap nhat bien moi)*.

### Doi mat khau Database sau khi da khoi tao Volume
Neu muon doi mat khau PostgreSQL khi database da chay:
```bash
docker exec -it soundwave-postgres psql -U <ten_user> -d <ten_db>
```
Trong giao dien psql, nhap cau lenh:
```sql
ALTER USER music_user WITH PASSWORD 'MatKhauMoi!';
\q
```
Sau do sua `DB_PASS` trong file `.env` va chay:
```bash
docker compose up -d
```

### Sao luu va Phuc hoi Du lieu (Backup & Restore)
```bash
# Backup toan bo co so du lieu ra file .sql
docker exec soundwave-postgres pg_dump -U <ten_user> -d <ten_db> > ~/backup_soundwave_$(date +%F).sql

# Backup toan bo file nhac upload tu container ra VPS
docker cp soundwave-webapp:/usr/local/tomcat/uploads ~/backup_uploads_$(date +%F)

# Phuc hoi co so du lieu tu file .sql
cat backup_soundwave_2026-10-26.sql | docker exec -i soundwave-postgres psql -U <ten_user> -d <ten_db>
```

### Reset toan bo Database ve trang thai ban dau
Neu muon xoa trang du lieu de chay lai tu dau voi file `init.sql`:
```bash
cd ~/MusicStreamingPlaylistManager
docker compose down -v
docker compose up -d
```

---

## 4. Xu ly su co thuong gap (Troubleshooting)

| Hien tuong | Nguyen nhan | Cach xu ly |
| :--- | :--- | :--- |
| `502 Bad Gateway` tren trinh duyet | Container `soundwave-webapp` chua khoi dong xong hoac bi dung | Chay `docker ps` kiem tra container, xem log bang `docker logs soundwave-webapp` |
| `-bash: !': event not found` | Mat khau chua dau `!` khi dat trong dau ngoac kep `"..."` tren terminal Linux | Dung giao dien `psql` truc tiep hoac dat chuoi trong dau ngoac don `'...'` |
| `404 Not Found` khi vao domain | Nginx chua duoc reload hoac chua tao symbolic link vao `sites-enabled` | Chay `nginx -t` roi `systemctl reload nginx` |
| `Connection refused` vao database | Postgres chua san sang hoac sai port tren host | Kiem tra port `5436`, kiem tra trang thai healthy cua `soundwave-postgres` |
