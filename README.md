# Microservice-Based Booking Application  

📌 *This project is a microservice-based booking application designed for scheduling beauty salon appointments. It consists of multiple microservices and a web interface, facilitating seamless communication and service orchestration.*  

## 🏰️ Project Structure  
The application is divided into several components, each serving a distinct role:  

### 📂 `micro-booking/` (Java Microservices)  
This directory contains the core microservices of the application, built in **Java**.  

- **🛡️ Gateway (brama)**  
  - Acts as an API gateway, receiving requests from the web interface and routing them to the appropriate microservices.  
  - Communicates with the **Availability Service** to check beautician availability.  
  - Ensures secure and efficient communication between services.  

- **📅 Availability Service (availability)**  
  - Checks the availability of a beautician at a specific date and time.  
  - Responds to availability queries from the gateway.  
  - Sends availability data to the **Visit Service** and **Discount Service** when needed.  

- **📌 Visit Service (visit)**  
  - Manages appointment bookings.  
  - Handles visit scheduling and ensures conflict-free reservations.  
  - Communicates with the **Discount Service** to finalize booking details.  

- **🎟️ Discount Service (discount)**  
  - Determines discount eligibility based on a customer's loyalty card.  
  - Processes discount requests and sends the final booking confirmation or an error message back to the **Gateway**.  

### 🎨 `flask-webapp/` (Web Interface)  
- A **Flask-based** web application serving as the graphical user interface (GUI).  
- Communicates directly with the **gateway** to interact with the microservices.  
- Allows users to check beautician availability, book appointments, and apply discounts.  

## 🔗 Communication Flow  
1. The **webapp** sends user requests to the **gateway**.  
2. The **gateway** forwards availability requests to the **Availability Service**.  
3. The **Availability Service** communicates with the **Visit Service**.
4. The **Visit Service** communicates with the **Discount Service** to validate booking details.  
5. The **Visit Service** processes appointment scheduling.  
6. The **Discount Service** determines the final discount eligibility and confirms or rejects the booking.  
7. The **Discount Service** sends the final booking confirmation or an error response back to the **Gateway**.  
8. The **Gateway** forwards the response to the **webapp**, ensuring a seamless user experience.  

## ⚙️ Requirements  
To run the project, you need the following environments and dependencies:  

### 🔧 Backend (Java Microservices)  
- **Java 17+**  
- **Spring Boot** (for microservices)  
- **Spring Cloud Gateway** (for the API gateway)  
- **Spring Data JPA** (for data persistence)  
- **PostgreSQL/MySQL** (for database management)  
- **Docker** (optional, for containerized deployment)  

### 🎨 Frontend (Flask Web App)  
- **Python 3.9+**  
- **Flask** (web framework)  
- **Flask-RESTful** (for API communication)  
- **Bootstrap** (for UI design)  

## 🚀 Running the Application  

### 1️⃣ Start Backend Services  
Ensure you have **Java and Maven** installed. Run the microservices using:  

```sh  
cd micro-booking  
mvn clean install  
mvn spring-boot:run  
```

Alternatively, use **Docker Compose**:  
```sh  
docker-compose up  
```

### 2️⃣ Start Frontend Web Interface  
Navigate to the **Flask WebApp** directory and start the server:  

```sh  
cd flask-webapp  
pip install -r requirements.txt  
python app.py  
```

## 🔥 Features  
✅ Check beautician availability 📅  
✅ Book beauty salon visits 💆‍♀️  
✅ Apply loyalty card discounts 🎟️  
✅ Secure and scalable microservice architecture ⚡  

## ✍️ Author  
📌 **Project developed by:** *Julia Podsadna*  

