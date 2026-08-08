# Study Notion LMS - Setup Guide

This guide will help you set up both the frontend and backend of the Study Notion LMS application.

## Quick Start

### 1. Install Dependencies

**Frontend:**
```bash
npm install
```

**Backend:**
```bash
cd server
npm install
cd ..
```

### 2. Configure Environment Variables

#### Backend Configuration (`server/.env`)

Copy `server/.env.example` to `server/.env` and update with your credentials:

```env
MONGODB_URL=mongodb://localhost:27017/studynotion
PORT=5000
CORS_ORIGIN=["http://localhost:3000"]
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
CLOUD_NAME=your-cloudinary-cloud-name
API_KEY=your-cloudinary-api-key
API_SECRET=your-cloudinary-api-secret
RAZORPAY_KEY=your-razorpay-key-id
RAZORPAY_SECRET=your-razorpay-key-secret
MAIL_HOST=smtp.gmail.com
MAIL_USER=your-email@gmail.com
MAIL_PASS=your-app-specific-password
CONTACT_MAIL=contact@studynotion.com
FOLDER_NAME=StudyNotion
FOLDER_VIDEO=StudyNotion/Videos
```

#### Frontend Configuration (`.env`)

Copy `.env.example` to `.env` in the root directory:

```env
REACT_APP_BASE_URL=http://localhost:5000/api/v1
```

### 3. Start MongoDB

Make sure MongoDB is running:
- **Local MongoDB**: Start MongoDB service on your system
- **MongoDB Atlas**: Use your connection string in `MONGODB_URL`

### 4. Run the Application

**Option A: Run Both Together (Recommended)**
```bash
npm run dev
```

**Option B: Run Separately**

Terminal 1 - Backend:
```bash
cd server
npm run dev
```

Terminal 2 - Frontend:
```bash
npm start
```

### 5. Access the Application

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:5000

## Required Services Setup

### MongoDB
1. Install MongoDB locally or create a MongoDB Atlas account
2. Update `MONGODB_URL` in `server/.env`

### Cloudinary (for media uploads)
1. Sign up at https://cloudinary.com
2. Get your Cloud Name, API Key, and API Secret
3. Update in `server/.env`

### Razorpay (for payments)
1. Sign up at https://razorpay.com
2. Get your Key ID and Key Secret from the dashboard
3. Update in `server/.env`

### Email (Nodemailer)
1. For Gmail:
   - Enable 2-Factor Authentication
   - Generate App Password: https://support.google.com/accounts/answer/185833
   - Use App Password in `MAIL_PASS`
2. Update `MAIL_HOST`, `MAIL_USER`, and `MAIL_PASS` in `server/.env`

## Creating Admin Account

1. Sign up with a student or instructor account through the frontend
2. Connect to your MongoDB database
3. Find the user in the `users` collection
4. Change `accountType` field to `"Admin"`
5. Log in again - you'll now have admin access

## Troubleshooting

### Port Already in Use
- Change `PORT` in `server/.env` for backend
- Create `.env` with `PORT=3001` for frontend (or modify package.json)

### MongoDB Connection Failed
- Verify MongoDB is running
- Check connection string format
- For Atlas, ensure IP is whitelisted

### CORS Errors
- Ensure `CORS_ORIGIN` in `server/.env` matches your frontend URL
- Format: `["http://localhost:3000"]` (JSON array)

### Environment Variables Not Loading
- Ensure `.env` files are in correct locations:
  - Root: `.env` (frontend)
  - Server: `server/.env` (backend)
- Restart the development server after changing `.env` files

## Project Structure

```
Study-Notion-LMS/
├── src/                 # Frontend React application
├── server/              # Backend Node.js/Express application
│   ├── config/         # Configuration files
│   ├── controllers/    # Route controllers
│   ├── models/         # MongoDB models
│   ├── routes/         # API routes
│   └── index.js        # Server entry point
├── .env                 # Frontend environment variables
├── .env.example         # Frontend environment template
├── server/.env          # Backend environment variables
├── server/.env.example  # Backend environment template
└── package.json         # Frontend dependencies
```

## Available Scripts

### Frontend
- `npm start` - Start React development server (port 3000)
- `npm run build` - Build for production
- `npm run dev` - Run both frontend and backend concurrently

### Backend
- `npm start` - Start server with Node (production)
- `npm run dev` - Start server with Nodemon (development)

## Notes

- Categories must be created before adding courses (use Admin panel)
- Ensure all environment variables are set before starting the server
- Backend must be running for frontend API calls to work
