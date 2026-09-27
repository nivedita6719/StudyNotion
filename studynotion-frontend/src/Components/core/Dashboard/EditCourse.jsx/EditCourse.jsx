import React from 'react'
import { getFullDetailsOfCourse } from '../../../../services/operations/courseDetailsAPI';
import { setCourse, setEditCourse, setStep } from '../../../../slices/courseSlice';
import { useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { useState } from 'react';
import { useSelector } from 'react-redux';
import { useDispatch } from 'react-redux';
import RenderSteps from '../AddCourse/RenderSteps';

const EditCourse = () => {
    const {token} = useSelector((state) => state.auth);
    const {course} = useSelector((state) => state.course);
    const {courseId} = useParams();
    // Start `true`: RenderSteps reads course.courseContent/category etc. with no
    // null-guards, so it must never mount before the fetch below finishes —
    // that was crashing the whole page blank (no error boundary in this app).
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState(false);
    const dispatch = useDispatch();

    useEffect(() => {
        let isMounted = true;
        const populateCourse = async () => {
            setLoading(true);
            setLoadError(false);
            const result = await getFullDetailsOfCourse(courseId, token);
            if (!isMounted) return;
            if (result?.courseDetails) {
                dispatch(setCourse(result.courseDetails));
                dispatch(setEditCourse(true));
                dispatch(setStep(1));
            } else {
                setLoadError(true);
            }
            setLoading(false);
        }
        populateCourse();
        return () => { isMounted = false; };
    }, [courseId, token, dispatch]);

  return (
    <div className='mx-auto w-11/12 max-w-[1000px] py-10'>
        <h1 className='mb-14 text-3xl font-medium text-richblack-5'>Edit Course</h1>
        {
            loading ? <p className='text-richblack-100'>Loading...</p> :
            loadError || !course ? (
                <p className='text-pink-200'>Could not load this course. Go back to My Courses and try again.</p>
            ) : (
                <RenderSteps />
            )
        }
    </div>
  )
}

export default EditCourse