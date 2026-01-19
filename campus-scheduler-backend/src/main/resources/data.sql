-- =====================
-- Buildings
-- =====================
INSERT INTO buildings (code, name, address) VALUES ('SCI', 'Science Building', '100 University Ave');
INSERT INTO buildings (code, name, address) VALUES ('ENG', 'Engineering Hall', '200 Technology Drive');
INSERT INTO buildings (code, name, address) VALUES ('LIB', 'Main Library', '50 Campus Center');
INSERT INTO buildings (code, name, address) VALUES ('ART', 'Arts Center', '75 Creative Way');
INSERT INTO buildings (code, name, address) VALUES ('BUS', 'Business School', '300 Commerce Street');

-- =====================
-- Rooms
-- =====================
-- Science Building rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('101', 30, 'CLASSROOM', 'Projector, Whiteboard', 1);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('102', 25, 'LAB', 'Computers, Lab Equipment', 1);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('201', 100, 'LECTURE_HALL', 'Projector, Microphone, Recording', 1);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('202', 20, 'SEMINAR', 'Smart Board, Video Conferencing', 1);

-- Engineering Hall rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('110', 40, 'CLASSROOM', 'Projector, Whiteboard', 2);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('115', 30, 'LAB', 'Workstations, 3D Printers', 2);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('220', 150, 'LECTURE_HALL', 'Dual Projectors, Microphone', 2);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('225', 15, 'CONFERENCE', 'Video Conferencing, Whiteboard', 2);

-- Library rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('A1', 10, 'SEMINAR', 'Quiet Study, Whiteboard', 3);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('B2', 50, 'CLASSROOM', 'Projector, Computers', 3);

-- Arts Center rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('Studio1', 25, 'LAB', 'Art Supplies, Easels', 4);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('Theater', 200, 'LECTURE_HALL', 'Stage, Sound System, Lighting', 4);

-- Business School rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('301', 60, 'CLASSROOM', 'Projector, Case Study Boards', 5);
INSERT INTO rooms (room_number, capacity, type, features, building_id) VALUES ('305', 20, 'CONFERENCE', 'Video Conferencing, Display', 5);

-- =====================
-- Instructors
-- =====================
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('John', 'Smith', 'jsmith@university.edu', 'Computer Science', 'SCI-301');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Emily', 'Johnson', 'ejohnson@university.edu', 'Computer Science', 'SCI-302');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Michael', 'Brown', 'mbrown@university.edu', 'Mathematics', 'SCI-205');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Sarah', 'Davis', 'sdavis@university.edu', 'Physics', 'SCI-410');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('David', 'Wilson', 'dwilson@university.edu', 'Engineering', 'ENG-120');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Jennifer', 'Taylor', 'jtaylor@university.edu', 'Engineering', 'ENG-125');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Robert', 'Anderson', 'randerson@university.edu', 'Business', 'BUS-210');
INSERT INTO instructors (first_name, last_name, email, department, office_number) VALUES ('Lisa', 'Martinez', 'lmartinez@university.edu', 'Art', 'ART-105');

-- =====================
-- Courses
-- =====================
-- Computer Science courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('CS101', 'Introduction to Programming', 'Learn the basics of programming using Python', 3, 30, 'Computer Science', 1);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('CS201', 'Data Structures', 'Arrays, linked lists, trees, and graphs', 3, 25, 'Computer Science', 1);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('CS301', 'Algorithms', 'Algorithm design and analysis', 3, 25, 'Computer Science', 2);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('CS401', 'Database Systems', 'Relational databases and SQL', 3, 30, 'Computer Science', 2);

-- Mathematics courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('MATH101', 'Calculus I', 'Limits, derivatives, and integrals', 4, 40, 'Mathematics', 3);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('MATH201', 'Linear Algebra', 'Vectors, matrices, and linear transformations', 3, 35, 'Mathematics', 3);

-- Physics courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('PHYS101', 'Physics I', 'Mechanics and thermodynamics', 4, 50, 'Physics', 4);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('PHYS201', 'Physics II', 'Electricity and magnetism', 4, 40, 'Physics', 4);

-- Engineering courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('ENG101', 'Engineering Fundamentals', 'Introduction to engineering principles', 3, 45, 'Engineering', 5);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('ENG201', 'Circuit Analysis', 'Electrical circuit theory and analysis', 4, 30, 'Engineering', 6);

-- Business courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('BUS101', 'Introduction to Business', 'Overview of business concepts', 3, 60, 'Business', 7);
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('BUS301', 'Marketing Strategy', 'Strategic marketing planning', 3, 40, 'Business', 7);

-- Art courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id) VALUES ('ART101', 'Drawing Fundamentals', 'Basic drawing techniques', 3, 20, 'Art', 8);

-- =====================
-- Time Slots
-- =====================
-- Monday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '08:00:00', '09:15:00', 'Period 1');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '09:30:00', '10:45:00', 'Period 2');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '11:00:00', '12:15:00', 'Period 3');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '13:00:00', '14:15:00', 'Period 4');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '14:30:00', '15:45:00', 'Period 5');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('MONDAY', '16:00:00', '17:15:00', 'Period 6');

-- Tuesday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '08:00:00', '09:15:00', 'Period 1');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '09:30:00', '10:45:00', 'Period 2');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '11:00:00', '12:15:00', 'Period 3');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '13:00:00', '14:15:00', 'Period 4');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '14:30:00', '15:45:00', 'Period 5');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('TUESDAY', '16:00:00', '17:15:00', 'Period 6');

-- Wednesday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '08:00:00', '09:15:00', 'Period 1');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '09:30:00', '10:45:00', 'Period 2');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '11:00:00', '12:15:00', 'Period 3');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '13:00:00', '14:15:00', 'Period 4');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '14:30:00', '15:45:00', 'Period 5');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('WEDNESDAY', '16:00:00', '17:15:00', 'Period 6');

-- Thursday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '08:00:00', '09:15:00', 'Period 1');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '09:30:00', '10:45:00', 'Period 2');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '11:00:00', '12:15:00', 'Period 3');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '13:00:00', '14:15:00', 'Period 4');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '14:30:00', '15:45:00', 'Period 5');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('THURSDAY', '16:00:00', '17:15:00', 'Period 6');

-- Friday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('FRIDAY', '08:00:00', '09:15:00', 'Period 1');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('FRIDAY', '09:30:00', '10:45:00', 'Period 2');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('FRIDAY', '11:00:00', '12:15:00', 'Period 3');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('FRIDAY', '13:00:00', '14:15:00', 'Period 4');
INSERT INTO time_slots (day_of_week, start_time, end_time, label) VALUES ('FRIDAY', '14:30:00', '15:45:00', 'Period 5');
