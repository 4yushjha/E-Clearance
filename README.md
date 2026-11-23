# 🎓 E-Clearance Management System

A web-based **E-Clearance Management System** developed using Java Spring Boot and MySQL to digitize the college clearance process for students.

The system allows students to submit clearance requests, department staff to verify them, and administrators to monitor the overall clearance process. Students can track their clearance status and download their clearance certificate after all required departments approve their requests.

---

## 📌 Project Overview

In the traditional college clearance process, students have to visit different departments physically to obtain signatures and approvals before completing their graduation or leaving the institution.

This process can be time-consuming, difficult to track, and dependent on paperwork.

The **E-Clearance Management System** provides a centralized digital platform where students can complete the clearance process online.

## 🎯 Objectives

- Digitize the traditional college clearance process.
- Reduce paperwork and manual effort.
- Provide department-wise clearance verification.
- Allow students to track their clearance status.
- Maintain a history of rejected and resubmitted requests.
- Generate a downloadable clearance certificate after successful verification.
- Provide administrators with centralized system monitoring.

---

## ✨ Key Features

### 👨‍🎓 Student Module
- Secure login to the student dashboard.
- Submit a clearance request.
- Send clearance requests to required departments.
- View department-wise clearance status.
- Track pending, approved, and rejected requests.
- Resubmit requests rejected by a department.
- View verification history and attempt numbers.
- Download the clearance certificate after all departments approve the request.

### 👨‍💼 Staff Module
- Staff login.
- View clearance requests assigned to their department.
- Review student clearance requests.
- Approve or reject requests.
- Add remarks during verification.
- View department-specific verification tasks.

### 🛡️ Admin Module
- Admin login.
- View the overall system dashboard.
- Monitor students, staff, and departments.
- View clearance statistics.
- Monitor the clearance process centrally.

### 📄 Clearance Certificate
- Automatically generate a PDF clearance certificate.
- Allow students to download the certificate after successful completion of all required verifications.
- Include student and clearance-related information in the certificate.

### 🔄 Resubmission & Verification History
- Students can resubmit rejected department requests.
- Previously cleared departments remain cleared.
- Each resubmission creates a new verification history record.
- Attempt numbers are maintained for tracking previous submissions.

---

## 🔄 Project Workflow

1. Student logs into the system.
2. Student submits a clearance request.
3. Student sends the request to the required departments.
4. The respective department staff receives the request.
5. Staff reviews the request and approves or rejects it.
6. If rejected, the student can view the remarks and resubmit the request.
7. The system updates the verification status.
8. After all required departments approve the request, the overall clearance status becomes **CLEARED**.
9. The student can download the clearance certificate.

---

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend programming |
| Spring Boot | Backend application framework |
| Spring MVC | Web request handling |
| Spring Data JPA | Database operations |
| Hibernate | ORM |
| MySQL 8 | Relational database |
| Thymeleaf | Dynamic HTML pages |
| HTML5 | Frontend structure |
| CSS3 | Frontend styling |
| Maven | Dependency management |
| OpenPDF | PDF certificate generation |
| Eclipse IDE | Development environment |

---

## 🏗️ System Architecture

The application follows a layered architecture.

```text
              User Interface
             (HTML, CSS, Thymeleaf)
                      |
                      |
                Controller Layer
                      |
                      |
                 Service Layer
                      |
                      |
               Repository Layer
                      |
                      |
                MySQL Database
```

### Main Application Layers

- **Controller Layer:** Handles HTTP requests and responses.
- **Service Layer:** Contains business logic and clearance processing.
- **Repository Layer:** Performs database operations using Spring Data JPA.
- **Entity Layer:** Represents database tables.
- **Thymeleaf Templates:** Display dynamic web pages.

---

## 🗄️ Main Database Entities

The project uses MySQL to store application data.

| Entity | Description |
|---|---|
| User | Stores login and user information |
| Student | Stores student academic information |
| Staff | Stores staff information |
| Department | Stores department details |
| ClearanceAuthority | Stores departments responsible for clearance |
| ClearanceRequest | Stores student clearance requests |
| ClearanceVerification | Stores current department verification status |
| ClearanceVerificationHistory | Maintains previous verification attempts |

### Clearance Status

The system uses the following statuses:

- `PENDING` – Request is waiting for verification.
- `UNDER_REVIEW` – Request is being reviewed.
- `CLEARED` – Request has been approved.
- `REJECTED` – Request has been rejected.

---

## ⚙️ Installation & Setup

Follow these steps to run the project locally.

### 1. Clone the Repository

```bash
git clone https://github.com/4yushjha/digital-college-clearance.git
```

### 2. Open the Project

Open the cloned project in Eclipse IDE or another IDE that supports Maven and Spring Boot.

### 3. Configure MySQL

Start your MySQL server and create the database:

```sql
CREATE DATABASE college_clearance;
```

### 4. Configure Database Connection

Open:

`src/main/resources/application.properties`

Configure your MySQL username and password:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/college_clearance
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Replace the username and password with your own MySQL credentials.

### 5. Update Maven Dependencies

In Eclipse:

1. Right-click the project.
2. Select **Maven → Update Project**.
3. Select the project.
4. Click **OK**.

### 6. Run the Application

Locate the main Spring Boot application class and run it as:

**Run As → Spring Boot App**

### 7. Open the Application

Open your browser and visit:

```text
http://localhost:8080/login
```

The login page should appear.

---

## 🧪 Testing

The following scenarios have been implemented and tested during development:

- Student login and dashboard access.
- Staff login and department-specific dashboard access.
- Admin dashboard access.
- Student clearance request submission.
- Department-wise approval and rejection.
- Rejection remarks display.
- Resubmission of rejected requests.
- Verification history and attempt tracking.
- Preservation of previously approved departments.
- Overall clearance status calculation.
- PDF clearance certificate generation and download.

---

## 🔮 Future Scope

The project can be improved further with the following features:

- Password encryption using BCrypt.
- Spring Security-based authentication and authorization.
- Email notifications for clearance status updates.
- Digital signatures for department approvals.
- Cloud deployment for online access.
- Improved reporting and analytics for administrators.
- Mobile-friendly interface and mobile application support.

---

## 👨‍💻 Developer

**Ayush Jha**

B.Tech – Computer Science and Engineering  
Meerut Institute of Technology, Meerut

### Connect With Me

- GitHub: [4yushjha](https://github.com/4yushjha)
- LinkedIn: [Ayush Jha](https://www.linkedin.com/in/ayush-jha-2a5531395/)
- LeetCode: [4yush-jha](https://leetcode.com/u/4yush-jha/)

---

## 📌 Project Information

| Detail | Information |
|---|---|
| Project Name | E-Clearance Management System |
| Project Type | B.Tech Minor Project |
| Domain | Web Application Development |
| Backend | Java, Spring Boot |
| Frontend | HTML, CSS, Thymeleaf |
| Database | MySQL |
| IDE | Eclipse |
| Build Tool | Maven |

---

## ⭐ Support

If you find this project useful or interesting, consider giving this repository a ⭐ on GitHub.

**Thank you for visiting the E-Clearance Management System repository!**
