# ==========================================
# STAGE 1: Build mã nguồn bằng Maven & OpenJDK 8
# ==========================================
FROM maven:3.8.6-openjdk-8 AS build
WORKDIR /app

# Copy pom.xml và tải thư viện trước để tối ưu cache của Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy toàn bộ mã nguồn và biên dịch thành file .war
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: Khởi chạy ứng dụng bằng Tomcat 9
# ==========================================
FROM tomcat:9.0-jdk8-corretto
WORKDIR /usr/local/tomcat

# Dọn dẹp các ứng dụng mẫu mặc định của Tomcat
RUN rm -rf webapps/*

# Copy file .war vừa build ở Stage 1 vào webapps và đổi tên thành ROOT.war để chạy tại trang chủ (/)
COPY --from=build /app/target/MusicStreamingPlaylistManager-1.0-SNAPSHOT.war webapps/ROOT.war

# Mở cổng ứng dụng nội bộ
EXPOSE 8080

# Chạy server Tomcat
CMD ["catalina.sh", "run"]
