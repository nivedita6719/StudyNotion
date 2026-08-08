# Study Notion

Study Notion is an ED Tech (Education Technology) web application developed using the MERN stack.

## Note

This project is intended as a learning tool and can be used as a sample project for educational or personal projects.


***
## Features

* User Authentication: Study Notion provides secure user registration and authentication using JWT (JSON Web Tokens). Users can sign up, log in, and manage their 
  profiles with ease.
  
* Courses and Lessons: Instructors can create and edit created courses. Students can enroll in courses, access course materials, and track their progress.
* Progress Tracking: Study Notion allows students to track their progress in enrolled courses. They can view completed lessons, scores on quizzes and 
  assignments, and overall course progress.
  
* Payment Integration: Study Notion integrates with Razorpay for payment processing. Users can make secure payments for course enrollment and other services 
  using various payment methods supported by Razorpay.
  
* Search Functionality: Users can easily search for courses, lessons, and resources using the built-in search feature. This makes it convenient to find relevant 
  content quickly.
  
* Instructor Dashboard: Instructors have access to a comprehensive dashboard to view information about their courses, students, and income. The 
 dashboard provides charts and visualizations to present data clearly and intuitively. Instructors can monitor the total number of students enrolled in 
 each course, track course performance, and view their income generated from course sales.

  
***
## Screenshots
![Screenshot 2023-07-25 210844](https://github.com/himanshu8443/Study-Notion-master/assets/99420590/0cba8d5b-6a47-4721-ac9f-4279107c257e)
![Screenshot 2023-07-25 211309](https://github.com/himanshu8443/Study-Notion-master/assets/99420590/62c33b56-0bd5-4330-b1db-d41b80d9f69f)
<details>
  <summary>More screenshots</summary>
  
![Screenshot 2023-07-25 211451](https://github.com/himanshu8443/Study-Notion-master/assets/99420590/63f7163d-a74a-4e78-bc78-6b96b06073f9)
![image](https://github.com/himanshu8443/Study-Notion-master/assets/99420590/59d1d8c2-2824-45bb-a2f7-6f5dc234895c)
</details>

***

## Important
* Backend is  in the server folder.
* Before uploading courses and anything create the categories e.g. web dev, Python, etc. (without categories courses cannot be added). To create categories create an Admin account and go to dashboard then admin panel.
* To create an Admin account first sign up with a student or instructor account then go to your Database under the users model and change that 'accountType' to 'Admin'.


## Installation

### Prerequisites
- Node.js (v16.9 or higher)
- MongoDB (local installation or MongoDB Atlas account)
- npm or yarn package manager

### Step-by-Step Setup

1. **Clone the repository to your local machine.**
    ```sh
    git clone https://github.com/nivedita6719/Study-Notion-master.git
    cd Study-Notion-master
    ```

2. **Install Frontend Dependencies**
    ```sh
    npm install
    ```

3. **Install Backend Dependencies**
    ```sh
    cd server
    npm install
    cd ..
    ```

4. **Set up Environment Variables**

   **Backend Configuration (server/.env):**
   
   Copy `server/.env.example` to `server/.env` and update the following variables:
   
   ```env
   # Database Configuration
   MONGODB_URL=mongodb://localhost:27017/studynotion
   # Or use MongoDB Atlas: mongodb+srv://username:password@cluster.mongodb.net/studynotion
   
   # Server Configuration
   PORT=5000
   
   # CORS Configuration (JSON array format)
   CORS_ORIGIN=["http://localhost:3000"]
   
   # JWT Secret Key (generate a strong random string)
   JWT_SECRET=your-super-secret-jwt-key-change-this-in-production
   
   # Cloudinary Configuration (for image/video uploads)
   # Sign up at https://cloudinary.com to get these credentials
   CLOUD_NAME=your-cloudinary-cloud-name
   API_KEY=your-cloudinary-api-key
   API_SECRET=your-cloudinary-api-secret
   
   # Razorpay Configuration (for payments)
   # Sign up at https://razorpay.com to get these credentials
   RAZORPAY_KEY=your-razorpay-key-id
   RAZORPAY_SECRET=your-razorpay-key-secret
   
   # Email Configuration (Nodemailer)
   # For Gmail, use App Password: https://support.google.com/accounts/answer/185833
   MAIL_HOST=smtp.gmail.com
   MAIL_USER=your-email@gmail.com
   MAIL_PASS=your-app-specific-password
   
   # Contact Email
   CONTACT_MAIL=contact@studynotion.com
   
   # Cloudinary Folder Names
   FOLDER_NAME=StudyNotion
   FOLDER_VIDEO=StudyNotion/Videos
   ```

   **Frontend Configuration (.env):**
   
   Copy `.env.example` to `.env` in the root directory:
   
   ```env
   # Backend API Base URL
   REACT_APP_BASE_URL=http://localhost:5000/api/v1
   ```

5. **Start MongoDB**
   
   Make sure MongoDB is running on your system. If using MongoDB Atlas, ensure your connection string is correct in the `.env` file.

6. **Run the Application**

   **Option 1: Run Both Frontend and Backend Together (Recommended)**
   ```sh
   npm run dev
   ```
   This will start both the frontend (port 3000) and backend (port 5000) concurrently.

   **Option 2: Run Separately**
   
   **Terminal 1 - Backend:**
   ```sh
   cd server
   npm run dev
   ```
   
   **Terminal 2 - Frontend:**
   ```sh
   npm start
   ```

7. **Access the Application**
   - Frontend: Open [`http://localhost:3000`](http://localhost:3000) in your browser
   - Backend API: Available at [`http://localhost:5000`](http://localhost:5000)

### Important Notes

- **Database Setup**: Ensure MongoDB is running before starting the backend server
- **Admin Account**: To create an Admin account:
  1. Sign up with a student or instructor account
  2. Go to your MongoDB database
  3. Find the user in the `users` collection
  4. Change the `accountType` field to `"Admin"`
- **Categories**: Before uploading courses, create categories (e.g., web dev, Python, etc.) through the Admin panel. Courses cannot be added without categories.

### Troubleshooting

- **Port Already in Use**: If port 3000 or 5000 is already in use, you can change them in:
  - Frontend: Create `.env` with `PORT=3001` (or modify `package.json` scripts)
  - Backend: Change `PORT` in `server/.env`
  
- **MongoDB Connection Error**: Verify your MongoDB connection string and ensure MongoDB is running

- **CORS Errors**: Make sure `CORS_ORIGIN` in `server/.env` includes your frontend URL

- **Environment Variables Not Loading**: Ensure `.env` files are in the correct directories:
  - Root: `.env` (for frontend)
  - Server: `server/.env` (for backend)

The project is set up to use `postcss-cli` to process your CSS files. You can add your own `tailwind.config.js` file to customize your Tailwind setup.
