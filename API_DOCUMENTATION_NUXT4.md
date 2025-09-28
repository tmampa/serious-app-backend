# Library Management System - API Documentation for Nuxt 4

## Overview
This document provides comprehensive API documentation for the Library Management System backend, specifically designed for integration with your Nuxt 4 frontend application. The API provides endpoints for authentication, book management, borrowing operations, student management, and administrative functions.

## Base URL
```
https://your-domain.com/api
```

## Authentication
All protected endpoints require JWT authentication. Include the JWT token in the Authorization header:
```javascript
headers: {
  'Authorization': `Bearer ${token}`
}
```

## Nuxt 4 Integration Examples
This documentation includes specific examples for Nuxt 4 using the new `$fetch` API and composables.

---

## 🔐 Authentication Endpoints

### Login
**Endpoint:** `POST /api/auth/login`

**Description:** Authenticates a user and returns a JWT token with role information.

**Request Body:**
```typescript
interface LoginRequest {
  username: string;
  password: string;
}
```

**Response:**
```typescript
interface AuthenticationResponse {
  accessToken: string;
  role: string; // "[ROLE_ADMIN]" or "[ROLE_STUDENT]"
}
```

**Nuxt 4 Example:**
```typescript
// composables/useAuth.ts
export const useAuth = () => {
  const login = async (credentials: LoginRequest) => {
    try {
      const response = await $fetch<AuthenticationResponse>('/api/auth/login', {
        method: 'POST',
        body: credentials
      });
      
      // Store token in Nuxt storage or cookie
      const token = useCookie('auth-token');
      token.value = response.accessToken;
      
      return response;
    } catch (error) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Invalid credentials'
      });
    }
  };
  
  return { login };
};
```

### Get Current User Details
**Endpoint:** `GET /api/auth/details`

**Response:**
```typescript
interface UserDetails {
  username: string;
  authorities: Array<{authority: string}>;
  // Additional user properties based on role
}
```

**Nuxt 4 Example:**
```typescript
// composables/useAuth.ts
const getCurrentUser = async () => {
  const token = useCookie('auth-token');
  
  return await $fetch<UserDetails>('/api/auth/details', {
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

### Update Password
**Endpoint:** `PUT /api/auth/{userId}/update-password`

**Request Body:**
```typescript
interface User {
  password: string;
}
```

**Nuxt 4 Example:**
```typescript
const updatePassword = async (userId: number, newPassword: string) => {
  const token = useCookie('auth-token');
  
  return await $fetch(`/api/auth/${userId}/update-password`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token.value}`
    },
    body: { password: newPassword }
  });
};
```

---

## 📚 Admin - Book Management Endpoints

### Create Book
**Endpoint:** `POST /api/admin/books`
**Authorization:** Admin role required

**Request Body:**
```typescript
interface BookRequest {
  title: string;
  author: string;
  isbn: string;
  grade: string;
  barcode: string;
  publishedYear: number;
  publisher: string;
  pages: number;
  language: string;
  genre: string;
  description: string;
  price: number;
  coverImageUrl: string;
}
```

**Response:**
```typescript
interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  grade: string;
  barcode: string;
  publishedYear: number;
  publisher: string;
  pages: number;
  language: string;
  genre: string;
  description: string;
  price: number;
  coverImageUrl: string;
  available: boolean;
}
```

**Nuxt 4 Example:**
```typescript
// composables/useBooks.ts
export const useBooks = () => {
  const createBook = async (bookData: BookRequest) => {
    const token = useCookie('auth-token');
    
    return await $fetch<Book>('/api/admin/books', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: bookData
    });
  };
  
  return { createBook };
};
```

### Get All Books
**Endpoint:** `GET /api/admin/books`
**Authorization:** Admin role required

**Response:**
```typescript
interface BookResponse {
  id: number;
  title: string;
  author: string;
  isbn: string;
}[]
```

**Nuxt 4 Example:**
```typescript
const getAllBooks = async () => {
  const token = useCookie('auth-token');
  
  return await $fetch<BookResponse[]>('/api/admin/books', {
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

---

## 📖 Admin - Borrowing Management Endpoints

### Create Borrow Record
**Endpoint:** `POST /api/admin/books/borrow/{bookTitle}`
**Authorization:** Admin role required

**Path Parameters:**
- `bookTitle` (string): URL-encoded book title

**Request Body:**
```typescript
interface UserRequest {
  studentNumber: string;
  // Additional user identification fields
}
```

**Response:**
```typescript
interface BorrowRecordResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentNumber: string;
  bookId: number;
  bookTitle: string;
  author: string;
  borrowDate: string; // ISO date string
}
```

**Nuxt 4 Example:**
```typescript
// composables/useBorrowing.ts
export const useBorrowing = () => {
  const createBorrowRecord = async (bookTitle: string, userRequest: UserRequest) => {
    const token = useCookie('auth-token');
    const encodedTitle = encodeURIComponent(bookTitle);
    
    return await $fetch<BorrowRecordResponse>(`/api/admin/books/borrow/${encodedTitle}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: userRequest
    });
  };
  
  return { createBorrowRecord };
};
```

### Upload Borrow Images
**Endpoint:** `POST /api/admin/books/upload-images/{recordId}`
**Authorization:** Admin role required

**Path Parameters:**
- `recordId` (number): Borrowing record ID

**Request Body:** `multipart/form-data`
- `images`: Array of image files

**Response:**
```typescript
interface BorrowingRecord {
  id: number;
  student: {
    id: number;
    firstName: string;
    lastName: string;
    studentNumber: number;
  };
  book: {
    id: number;
    title: string;
    author: string;
  };
  borrowDate: string;
  images?: string[]; // Array of image URLs
  tags?: string[]; // AI-generated tags
}
```

**Nuxt 4 Example:**
```typescript
const uploadBorrowImages = async (recordId: number, files: File[]) => {
  const token = useCookie('auth-token');
  const formData = new FormData();
  
  files.forEach(file => {
    formData.append('images', file);
  });
  
  return await $fetch<BorrowingRecord>(`/api/admin/books/upload-images/${recordId}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token.value}`
    },
    body: formData
  });
};
```

### Return Book
**Endpoint:** `POST /api/admin/books/return/{studentNumber}/{bookTitle}`
**Authorization:** Admin role required

**Path Parameters:**
- `studentNumber` (number): Student's number
- `bookTitle` (string): URL-encoded book title

**Request Body:** `multipart/form-data`
- `images`: Array of return condition images

**Response:**
```typescript
number // Fine amount calculated
```

**Nuxt 4 Example:**
```typescript
const returnBook = async (studentNumber: number, bookTitle: string, images: File[]) => {
  const token = useCookie('auth-token');
  const formData = new FormData();
  
  images.forEach(image => {
    formData.append('images', image);
  });
  
  const encodedTitle = encodeURIComponent(bookTitle);
  
  return await $fetch<number>(`/api/admin/books/return/${studentNumber}/${encodedTitle}`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token.value}`
    },
    body: formData
  });
};
```

### Get All Borrow Records
**Endpoint:** `GET /api/admin/borrow-records`
**Authorization:** Admin role required

**Response:**
```typescript
interface BorrowRecordResponse {
  id: number;
  studentId: number;
  studentName: string;
  studentNumber: string;
  bookTitle: string;
  borrowDate: string;
  returnDate?: string;
}[]
```

**Nuxt 4 Example:**
```typescript
const getAllBorrowRecords = async () => {
  const token = useCookie('auth-token');
  
  return await $fetch<BorrowRecordResponse[]>('/api/admin/borrow-records', {
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

---

## 👥 Admin - Student Management Endpoints

### Create Student
**Endpoint:** `POST /api/admin/create-student`
**Authorization:** Admin role required

**Request Body:**
```typescript
interface StudentRequest {
  username: string;
  email: string;
  firstNames: string;
  lastName: string;
  studentNumber: number;
  address: string;
  outstandingFines?: number;
  parents: ParentRequest[];
}

interface ParentRequest {
  name: string;
  email: string;
  relationship: string; // "MOTHER", "FATHER", "GUARDIAN"
}
```

**Response:**
```typescript
interface StudentResponse {
  id: number;
  fullName: string;
  studentNumber: number;
  username: string;
  role: string;
  parents: ParentResponse[];
  address: string;
  outstandingFines: number;
  borrowedBooks: any[]; // Array of borrowed books
}

interface ParentResponse {
  id: number;
  name: string;
  email: string;
  relationship: string;
}
```

**Nuxt 4 Example:**
```typescript
// composables/useStudents.ts
export const useStudents = () => {
  const createStudent = async (studentData: StudentRequest) => {
    const token = useCookie('auth-token');
    
    return await $fetch<StudentResponse>('/api/admin/create-student', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: studentData
    });
  };
  
  return { createStudent };
};
```

### Get All Students
**Endpoint:** `GET /api/admin/students`
**Authorization:** Admin role required

**Response:**
```typescript
StudentResponse[] // Array of student responses
```

**Nuxt 4 Example:**
```typescript
const getAllStudents = async () => {
  const token = useCookie('auth-token');
  
  return await $fetch<StudentResponse[]>('/api/admin/students', {
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

### Get Student Fines
**Endpoint:** `GET /api/admin/students/{studentId}/fines`
**Authorization:** Admin role required

**Path Parameters:**
- `studentId` (number): Student's ID

**Response:**
```typescript
number // Outstanding fine amount
```

**Nuxt 4 Example:**
```typescript
const getStudentFines = async (studentId: number) => {
  const token = useCookie('auth-token');
  
  return await $fetch<number>(`/api/admin/students/${studentId}/fines`, {
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

### Clear Student Fines
**Endpoint:** `PUT /api/admin/students/{studentId}/fines/clear`
**Authorization:** Admin role required

**Path Parameters:**
- `studentId` (number): Student's ID

**Response:**
```typescript
void // No content response
```

**Nuxt 4 Example:**
```typescript
const clearStudentFines = async (studentId: number) => {
  const token = useCookie('auth-token');
  
  return await $fetch<void>(`/api/admin/students/${studentId}/fines/clear`, {
    method: 'PUT',
    headers: {
      'Authorization': `Bearer ${token.value}`
    }
  });
};
```

---

## 👤 Admin - Admin Management Endpoints

### Create Admin
**Endpoint:** `POST /api/admin/create-admin`
**Authorization:** Admin role required

**Request Body:**
```typescript
interface AdminRequest {
  username: string;
  password: string;
  email: string;
  employeeId: string;
}
```

**Response:**
```typescript
interface Admin {
  id: number;
  username: string;
  email: string;
  employeeId: string;
  // Password is excluded from response
}
```

**Nuxt 4 Example:**
```typescript
// composables/useAdmins.ts
export const useAdmins = () => {
  const createAdmin = async (adminData: AdminRequest) => {
    const token = useCookie('auth-token');
    
    return await $fetch<Admin>('/api/admin/create-admin', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`,
        'Content-Type': 'application/json'
      },
      body: adminData
    });
  };
  
  return { createAdmin };
};
```

---

## 🛠️ Nuxt 4 Composables Integration

### Complete Auth Composable
```typescript
// composables/useAuth.ts
export const useAuth = () => {
  const token = useCookie('auth-token', {
    maxAge: 60 * 60 * 24 * 7, // 7 days
    httpOnly: true,
    secure: true,
    sameSite: 'strict'
  });

  const user = ref<UserDetails | null>(null);
  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => 
    user.value?.authorities?.some(auth => auth.authority === 'ROLE_ADMIN')
  );

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await $fetch<AuthenticationResponse>('/api/auth/login', {
        method: 'POST',
        body: credentials
      });
      
      token.value = response.accessToken;
      await fetchCurrentUser();
      
      return response;
    } catch (error) {
      throw createError({
        statusCode: 401,
        statusMessage: 'Invalid credentials'
      });
    }
  };

  const fetchCurrentUser = async () => {
    if (!token.value) return;
    
    try {
      user.value = await $fetch<UserDetails>('/api/auth/details', {
        headers: {
          'Authorization': `Bearer ${token.value}`
        }
      });
    } catch (error) {
      // Token might be invalid, clear it
      token.value = null;
      user.value = null;
    }
  };

  const logout = () => {
    token.value = null;
    user.value = null;
    navigateTo('/login');
  };

  const updatePassword = async (userId: number, newPassword: string) => {
    return await $fetch(`/api/auth/${userId}/update-password`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token.value}`
      },
      body: { password: newPassword }
    });
  };

  return {
    token: readonly(token),
    user: readonly(user),
    isLoggedIn,
    isAdmin,
    login,
    logout,
    fetchCurrentUser,
    updatePassword
  };
};
```

### Complete Library Management Composable
```typescript
// composables/useLibrary.ts
export const useLibrary = () => {
  const { token } = useAuth();
  
  const createAuthHeaders = () => ({
    'Authorization': `Bearer ${token.value}`,
    'Content-Type': 'application/json'
  });

  // Book Management
  const createBook = async (bookData: BookRequest) => {
    return await $fetch<Book>('/api/admin/books', {
      method: 'POST',
      headers: createAuthHeaders(),
      body: bookData
    });
  };

  const getAllBooks = async () => {
    return await $fetch<BookResponse[]>('/api/admin/books', {
      headers: createAuthHeaders()
    });
  };

  // Borrowing Management
  const createBorrowRecord = async (bookTitle: string, userRequest: UserRequest) => {
    const encodedTitle = encodeURIComponent(bookTitle);
    return await $fetch<BorrowRecordResponse>(`/api/admin/books/borrow/${encodedTitle}`, {
      method: 'POST',
      headers: createAuthHeaders(),
      body: userRequest
    });
  };

  const uploadBorrowImages = async (recordId: number, files: File[]) => {
    const formData = new FormData();
    files.forEach(file => formData.append('images', file));
    
    return await $fetch<BorrowingRecord>(`/api/admin/books/upload-images/${recordId}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`
      },
      body: formData
    });
  };

  const returnBook = async (studentNumber: number, bookTitle: string, images: File[]) => {
    const formData = new FormData();
    images.forEach(image => formData.append('images', image));
    
    const encodedTitle = encodeURIComponent(bookTitle);
    return await $fetch<number>(`/api/admin/books/return/${studentNumber}/${encodedTitle}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token.value}`
      },
      body: formData
    });
  };

  const getAllBorrowRecords = async () => {
    return await $fetch<BorrowRecordResponse[]>('/api/admin/borrow-records', {
      headers: createAuthHeaders()
    });
  };

  // Student Management
  const createStudent = async (studentData: StudentRequest) => {
    return await $fetch<StudentResponse>('/api/admin/create-student', {
      method: 'POST',
      headers: createAuthHeaders(),
      body: studentData
    });
  };

  const getAllStudents = async () => {
    return await $fetch<StudentResponse[]>('/api/admin/students', {
      headers: createAuthHeaders()
    });
  };

  const getStudentFines = async (studentId: number) => {
    return await $fetch<number>(`/api/admin/students/${studentId}/fines`, {
      headers: createAuthHeaders()
    });
  };

  const clearStudentFines = async (studentId: number) => {
    return await $fetch<void>(`/api/admin/students/${studentId}/fines/clear`, {
      method: 'PUT',
      headers: createAuthHeaders()
    });
  };

  // Admin Management
  const createAdmin = async (adminData: AdminRequest) => {
    return await $fetch<Admin>('/api/admin/create-admin', {
      method: 'POST',
      headers: createAuthHeaders(),
      body: adminData
    });
  };

  return {
    // Book Management
    createBook,
    getAllBooks,
    
    // Borrowing Management
    createBorrowRecord,
    uploadBorrowImages,
    returnBook,
    getAllBorrowRecords,
    
    // Student Management
    createStudent,
    getAllStudents,
    getStudentFines,
    clearStudentFines,
    
    // Admin Management
    createAdmin
  };
};
```

---

## 🔧 Nuxt 4 Plugin Setup

### API Plugin
```typescript
// plugins/api.client.ts
export default defineNuxtPlugin(() => {
  const { token } = useAuth();

  // Global interceptor for authenticated requests
  $fetch.create({
    onRequest({ options }) {
      if (token.value && options.headers) {
        options.headers = {
          ...options.headers,
          'Authorization': `Bearer ${token.value}`
        };
      }
    },
    
    onResponseError({ response }) {
      if (response.status === 401) {
        // Token expired or invalid
        const { logout } = useAuth();
        logout();
      }
    }
  });
});
```

---

## 🚨 Error Handling

### Common HTTP Status Codes
- `200 OK`: Request successful
- `201 Created`: Resource created successfully  
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Authentication required or invalid token
- `403 Forbidden`: Insufficient permissions (requires admin role)
- `404 Not Found`: Resource not found
- `413 Payload Too Large`: File size exceeds limit
- `415 Unsupported Media Type`: Invalid file format
- `500 Internal Server Error`: Server error

### Nuxt 4 Error Handling
```typescript
// composables/useApiError.ts
export const useApiError = () => {
  const handleApiError = (error: any) => {
    if (error.response) {
      const statusCode = error.response.status;
      
      switch (statusCode) {
        case 401:
          throw createError({
            statusCode: 401,
            statusMessage: 'Authentication required'
          });
        case 403:
          throw createError({
            statusCode: 403,
            statusMessage: 'Insufficient permissions'
          });
        case 404:
          throw createError({
            statusCode: 404,
            statusMessage: 'Resource not found'
          });
        default:
          throw createError({
            statusCode: statusCode,
            statusMessage: error.response.data?.message || 'An error occurred'
          });
      }
    }
    
    throw createError({
      statusCode: 500,
      statusMessage: 'Network error'
    });
  };
  
  return { handleApiError };
};
```

---

## 📧 Email Notifications

### Automatic Notifications
The system automatically sends email notifications for:

1. **Student Registration**: Login credentials sent to student and parents
2. **Book Borrowing**: Notification sent to parents with book details and condition images
3. **Book Return**: Notification sent to parents with return confirmation and any fines

### Default Recipients
If no parent emails are configured:
- `lucky.hlungs@gmail.com`
- `jonassmoloto@gmail.com`

---

## 🖼️ Image Processing Features

### Azure Computer Vision Integration
- Automatic image analysis for damage detection
- Tag generation for condition assessment
- Comparison between borrow and return images
- Damage-based fine calculation

### Fine Calculation
- Missing book cover: $90.00
- Coffee stain: $20.00
- Torn pages: $50.00
- Unknown damage: $9.99 (default)

### Blob Storage
- Secure cloud storage for all images
- Public access URLs for email notifications
- Automatic container creation
- Container naming: `{bookTitle}-{timestamp}`

---

## 🔒 Security Considerations

### JWT Token Security
- Store tokens in secure HTTP-only cookies
- Set appropriate expiration times
- Implement token refresh mechanism
- Clear tokens on logout

### File Upload Security
- Validate file types (JPEG, PNG only)
- Limit file sizes (10MB per image)
- Limit number of images (10 per request)
- Scan uploaded files for malware

### Role-Based Access
- All admin endpoints require ROLE_ADMIN
- Implement middleware to check permissions
- Validate user roles on each request

---

## 📊 Rate Limiting
- Maximum 100 requests per minute per IP
- File upload endpoints: 10 requests per minute
- Bulk operations: 5 requests per minute

---

## 🔄 Data Validation

### Required Fields
All API endpoints validate required fields and return appropriate error messages for missing or invalid data.

### Student Number Validation
- Must be unique across the system
- Used for book borrowing and returning operations

### Email Validation
- Valid email format required for all email fields
- Used for notifications and login credentials

---

This documentation provides everything needed to integrate your Nuxt 4 frontend with the Library Management System API. Use the provided composables and examples to build a robust and type-safe frontend application.
