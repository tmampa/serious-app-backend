# Library Management System API

## Building and Running the Application

1. Build the application:
```bash
mvn clean install
```

2. Run the application:
```bash
mvn spring-boot:run
```

## API Documentation

The API documentation is available through Swagger UI when the application is running. You can access it at:

```
http://localhost:8080/swagger-ui.html
```

### Available API Endpoints

#### Student Endpoints (`/api/student`)
- GET `/api/student/profile` - Get student profile
- GET `/api/student/borrowed-books` - List currently borrowed books
- GET `/api/student/outstanding-fines` - Check outstanding fines
- GET `/api/student/borrowing-history` - View borrowing history

#### Admin Endpoints (`/api/admin`)
- POST `/api/admin/books` - Create a new book
- POST `/api/admin/books/borrow/{bookTitle}` - Create a borrowing record
- POST `/api/admin/books/upload-images/{recordId}` - Upload book images
- POST `/api/admin/books/return/{studentNumber}/{bookTitle}` - Process book return
- GET `/api/admin/students/{studentId}/fines` - Get student fines
- PUT `/api/admin/students/{studentId}/fines/clear` - Clear student fines

### Authentication

The API uses OAuth2 for authentication. All endpoints except the documentation and login-related URLs require authentication.

### Role-Based Access

- Student endpoints require ROLE_STUDENT
- Admin endpoints require ROLE_ADMIN

## For Frontend Engineers

1. All API endpoints are documented with:
   - Detailed descriptions
   - Required parameters
   - Request/response formats
   - Possible response codes
   - Authentication requirements

2. Testing the API:
   - Use the Swagger UI interface for testing endpoints
   - All secure endpoints require proper authentication
   - Include the Bearer token in the Authorization header

3. Error Handling:
   - 400: Bad Request - Invalid input
   - 401: Unauthorized - Not authenticated
   - 403: Forbidden - Not authorized
   - 404: Not Found - Resource doesn't exist
   - 500: Internal Server Error - Server-side error
