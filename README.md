# 💰 FinanceForge

> Modern expense tracking and financial management system built with enterprise-grade architecture

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![Oracle](https://img.shields.io/badge/Oracle-Database%2021c-red.svg)](https://www.oracle.com/database/)
[![React](https://img.shields.io/badge/React-18+-blue.svg)](https://reactjs.org/)
[![Development Status](https://img.shields.io/badge/Status-In%20Development-yellow.svg)]()

## 🎯 Project Overview

FinanceForge is a personal expense tracking system designed to replace spreadsheet-based financial management with a robust, enterprise-grade application. Built as both a practical tool and technical showcase, it demonstrates modern Java development practices combined with Oracle Database's powerful PL/SQL capabilities.

## ✨ Planned Features

### 💼 Core Functionality (In Development)

- 📊 **Expense Tracking** - Add, categorize, and manage personal expenses
- 🏷️ **Category Management** - Organize expenses with hierarchical categories
- 📈 **Budget Planning** - Set spending limits and track progress
- 📱 **Responsive Design** - Mobile-friendly interface for on-the-go tracking
- 📥 **Data Import** - Import existing expense data from CSV files

### 🤖 Future Enhancements

- 🧠 **Smart Categorization** - Auto-suggest categories based on patterns
- 📊 **Analytics Dashboard** - Spending insights and trend analysis
- 🔄 **Recurring Expenses** - Handle subscriptions and regular payments
- 🚨 **Budget Alerts** - Notifications for spending limits

## 🏗️ Architecture

### Technology Stack

```text
Backend:   Java 21 LTS + Spring Boot 3.5 + Maven
Database:  Oracle Database 21c XE + PL/SQL Packages
Frontend:  React 18 + TypeScript (Planned)
Security:  Spring Security + Environment-based secrets
Testing:   JUnit 5 + Spring Boot Test
```

### Fat Database Philosophy

FinanceForge implements a **database-centric architecture** where:

- 🧠 **Business Logic**: Implemented in Oracle PL/SQL packages
- 📊 **Data Processing**: Complex financial calculations in stored procedures  
- 🔒 **Data Integrity**: Database constraints and triggers enforce business rules
- 🚀 **Performance**: Optimized database operations with minimal network overhead
- 🎯 **API Layer**: Lightweight REST controllers focused on HTTP concerns

### Current Project Structure

```plaintext
financeforge/
├── src/main/java/com/financeforge/api/
│   ├── controller/             # REST Controllers (health check implemented)
│   ├── service/                # Service layer (planned)
│   ├── dto/                    # Data Transfer Objects (planned)
│   ├── entity/                 # JPA Entities (planned)
│   └── config/                 # Spring configuration
├── src/main/resources/
│   ├── application.yml         # Environment-based configuration ✅
│   └── db/migration/           # Flyway migrations (planned)
├── src/test/java/
│   └── DatabaseConnectionTest.java # Database connectivity tests ✅
└── docs/                       # Documentation and setup guides
```

## 🚀 Getting Started

### Prerequisites

- Java 21 LTS
- Docker (for Oracle Database)
- Maven 3.9+

### Current Setup (Completed)

1. **Oracle Database**: Docker container with Oracle XE 21c
2. **Database User**: Dedicated `financeforge` user with appropriate permissions
3. **Security**: Environment-based configuration (no hardcoded passwords)
4. **Health Checks**: Database connectivity monitoring
5. **Testing**: Automated database connection verification

### Quick Start

```bash
# Clone repository
git clone https://github.com/yourusername/financeforge.git
cd financeforge

# Start Oracle Database (Docker)
docker run -d \
  --name financeforge-oracle \
  -p 1521:1521 \
  -p 5500:5500 \
  -e ORACLE_PASSWORD=FinanceForge123! \
  -e ORACLE_DATABASE=FINANCEFORGE \
  -v oracle-data:/opt/oracle/oradata \
  gvenzl/oracle-xe:21-slim

# Set environment variables
export DB_USERNAME=financeforge
export DB_PASSWORD=FF_Secure_2024!
export DB_URL=jdbc:oracle:thin:@localhost:1521/XEPDB1

# Start application
mvn spring-boot:run
```

### Verify Setup

```bash
# Check health endpoint
curl http://localhost:8080/api/v1/health

# Run database tests
mvn test -D test=DatabaseConnectionTest
```

## 🧪 Testing

### Backend Testing

```bash
# Run all tests
mvn test

# Run specific database tests
mvn test -D test=DatabaseConnectionTest

# Run with specific profile
mvn test -D spring.profiles.active=dev
```

## 📚 Documentation

- [Development Tickets](docs/) - Detailed development progress and tasks

## 🎯 Project Goals

This project serves multiple purposes:

1. **Personal Tool** - Replace my current spreadsheet-based expense tracking
2. **Technical Showcase** - Demonstrate enterprise Java development skills
3. **Learning Platform** - Explore database-centric architecture and Oracle PL/SQL
4. **Portfolio Piece** - Show full-stack development capabilities with modern practices

## 🔧 Development Philosophy

- **Security First**: No hardcoded credentials, proper environment management
- **Enterprise Patterns**: Following Spring Boot best practices and enterprise architecture
- **Documentation**: Comprehensive documentation of setup and development process
- **Testing**: Automated testing for all critical functionality
- **Clean Architecture**: Clear separation of concerns with fat database design

## 🤝 Contributing

This is primarily a personal learning project, but suggestions and feedback are welcome through issues!

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

- **GitHub**: [Programming With Tyler](https://github.com/programmingwithtyler)

---

⭐ **Star this repo if you're interested in enterprise Java development with Oracle Database!**
