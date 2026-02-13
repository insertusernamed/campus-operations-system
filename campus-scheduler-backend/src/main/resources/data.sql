-- =====================
-- Buildings
-- =====================
INSERT INTO buildings (code, name, address)
SELECT 'SCI', 'Science Building', '100 University Ave'
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'SCI');
INSERT INTO buildings (code, name, address)
SELECT 'ENG', 'Engineering Hall', '200 Technology Drive'
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'ENG');
INSERT INTO buildings (code, name, address)
SELECT 'LIB', 'Main Library', '50 Campus Center'
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'LIB');
INSERT INTO buildings (code, name, address)
SELECT 'ART', 'Arts Center', '75 Creative Way'
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'ART');
INSERT INTO buildings (code, name, address)
SELECT 'BUS', 'Business School', '300 Commerce Street'
WHERE NOT EXISTS (SELECT 1 FROM buildings WHERE code = 'BUS');

-- =====================
-- Rooms
-- =====================
-- Science Building rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '101', 30, 'CLASSROOM', 'Projector, Whiteboard', b.id
FROM buildings b
WHERE b.code = 'SCI'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '101' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '102', 25, 'LAB', 'Computers, Lab Equipment', b.id
FROM buildings b
WHERE b.code = 'SCI'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '102' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '201', 100, 'LECTURE_HALL', 'Projector, Microphone, Recording', b.id
FROM buildings b
WHERE b.code = 'SCI'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '201' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '202', 20, 'SEMINAR', 'Smart Board, Video Conferencing', b.id
FROM buildings b
WHERE b.code = 'SCI'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '202' AND r.building_id = b.id);

-- Engineering Hall rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '110', 40, 'CLASSROOM', 'Projector, Whiteboard', b.id
FROM buildings b
WHERE b.code = 'ENG'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '110' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '115', 30, 'LAB', 'Workstations, 3D Printers', b.id
FROM buildings b
WHERE b.code = 'ENG'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '115' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '220', 150, 'LECTURE_HALL', 'Dual Projectors, Microphone', b.id
FROM buildings b
WHERE b.code = 'ENG'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '220' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '225', 15, 'CONFERENCE', 'Video Conferencing, Whiteboard', b.id
FROM buildings b
WHERE b.code = 'ENG'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '225' AND r.building_id = b.id);

-- Library rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT 'A1', 10, 'SEMINAR', 'Quiet Study, Whiteboard', b.id
FROM buildings b
WHERE b.code = 'LIB'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = 'A1' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT 'B2', 50, 'CLASSROOM', 'Projector, Computers', b.id
FROM buildings b
WHERE b.code = 'LIB'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = 'B2' AND r.building_id = b.id);

-- Arts Center rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT 'Studio1', 25, 'LAB', 'Art Supplies, Easels', b.id
FROM buildings b
WHERE b.code = 'ART'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = 'Studio1' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT 'Theater', 200, 'LECTURE_HALL', 'Stage, Sound System, Lighting', b.id
FROM buildings b
WHERE b.code = 'ART'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = 'Theater' AND r.building_id = b.id);

-- Business School rooms
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '301', 60, 'CLASSROOM', 'Projector, Case Study Boards', b.id
FROM buildings b
WHERE b.code = 'BUS'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '301' AND r.building_id = b.id);
INSERT INTO rooms (room_number, capacity, type, features, building_id)
SELECT '305', 20, 'CONFERENCE', 'Video Conferencing, Display', b.id
FROM buildings b
WHERE b.code = 'BUS'
  AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.room_number = '305' AND r.building_id = b.id);

-- =====================
-- Instructors
-- =====================
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'John', 'Smith', 'jsmith@university.edu', 'Computer Science', 'SCI-301'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'jsmith@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Emily', 'Johnson', 'ejohnson@university.edu', 'Computer Science', 'SCI-302'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'ejohnson@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Michael', 'Brown', 'mbrown@university.edu', 'Mathematics', 'SCI-205'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'mbrown@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Sarah', 'Davis', 'sdavis@university.edu', 'Physics', 'SCI-410'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'sdavis@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'David', 'Wilson', 'dwilson@university.edu', 'Engineering', 'ENG-120'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'dwilson@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Jennifer', 'Taylor', 'jtaylor@university.edu', 'Engineering', 'ENG-125'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'jtaylor@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Robert', 'Anderson', 'randerson@university.edu', 'Business', 'BUS-210'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'randerson@university.edu');
INSERT INTO instructors (first_name, last_name, email, department, office_number)
SELECT 'Lisa', 'Martinez', 'lmartinez@university.edu', 'Art', 'ART-105'
WHERE NOT EXISTS (SELECT 1 FROM instructors WHERE email = 'lmartinez@university.edu');

-- =====================
-- Courses
-- =====================
-- Computer Science courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'CS101', 'Introduction to Programming', 'Learn the basics of programming using Python', 3, 30, 'Computer Science', i.id
FROM instructors i
WHERE i.email = 'jsmith@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'CS101');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'CS201', 'Data Structures', 'Arrays, linked lists, trees, and graphs', 3, 25, 'Computer Science', i.id
FROM instructors i
WHERE i.email = 'jsmith@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'CS201');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'CS301', 'Algorithms', 'Algorithm design and analysis', 3, 25, 'Computer Science', i.id
FROM instructors i
WHERE i.email = 'ejohnson@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'CS301');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'CS401', 'Database Systems', 'Relational databases and SQL', 3, 30, 'Computer Science', i.id
FROM instructors i
WHERE i.email = 'ejohnson@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'CS401');

-- Mathematics courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'MATH101', 'Calculus I', 'Limits, derivatives, and integrals', 4, 40, 'Mathematics', i.id
FROM instructors i
WHERE i.email = 'mbrown@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'MATH101');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'MATH201', 'Linear Algebra', 'Vectors, matrices, and linear transformations', 3, 35, 'Mathematics', i.id
FROM instructors i
WHERE i.email = 'mbrown@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'MATH201');

-- Physics courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'PHYS101', 'Physics I', 'Mechanics and thermodynamics', 4, 50, 'Physics', i.id
FROM instructors i
WHERE i.email = 'sdavis@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'PHYS101');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'PHYS201', 'Physics II', 'Electricity and magnetism', 4, 40, 'Physics', i.id
FROM instructors i
WHERE i.email = 'sdavis@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'PHYS201');

-- Engineering courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'ENG101', 'Engineering Fundamentals', 'Introduction to engineering principles', 3, 45, 'Engineering', i.id
FROM instructors i
WHERE i.email = 'dwilson@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'ENG101');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'ENG201', 'Circuit Analysis', 'Electrical circuit theory and analysis', 4, 30, 'Engineering', i.id
FROM instructors i
WHERE i.email = 'jtaylor@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'ENG201');

-- Business courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'BUS101', 'Introduction to Business', 'Overview of business concepts', 3, 60, 'Business', i.id
FROM instructors i
WHERE i.email = 'randerson@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'BUS101');
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'BUS301', 'Marketing Strategy', 'Strategic marketing planning', 3, 40, 'Business', i.id
FROM instructors i
WHERE i.email = 'randerson@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'BUS301');

-- Art courses
INSERT INTO courses (code, name, description, credits, enrollment_capacity, department, instructor_id)
SELECT 'ART101', 'Drawing Fundamentals', 'Basic drawing techniques', 3, 20, 'Art', i.id
FROM instructors i
WHERE i.email = 'lmartinez@university.edu'
  AND NOT EXISTS (SELECT 1 FROM courses c WHERE c.code = 'ART101');

-- =====================
-- Time Slots
-- =====================
-- Monday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '08:00:00', '09:15:00', 'Period 1'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '08:00:00' AND t.end_time = '09:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '09:30:00', '10:45:00', 'Period 2'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '09:30:00' AND t.end_time = '10:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '11:00:00', '12:15:00', 'Period 3'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '11:00:00' AND t.end_time = '12:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '13:00:00', '14:15:00', 'Period 4'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '13:00:00' AND t.end_time = '14:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '14:30:00', '15:45:00', 'Period 5'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '14:30:00' AND t.end_time = '15:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'MONDAY', '16:00:00', '17:15:00', 'Period 6'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'MONDAY' AND t.start_time = '16:00:00' AND t.end_time = '17:15:00');

-- Tuesday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '08:00:00', '09:15:00', 'Period 1'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '08:00:00' AND t.end_time = '09:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '09:30:00', '10:45:00', 'Period 2'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '09:30:00' AND t.end_time = '10:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '11:00:00', '12:15:00', 'Period 3'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '11:00:00' AND t.end_time = '12:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '13:00:00', '14:15:00', 'Period 4'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '13:00:00' AND t.end_time = '14:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '14:30:00', '15:45:00', 'Period 5'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '14:30:00' AND t.end_time = '15:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'TUESDAY', '16:00:00', '17:15:00', 'Period 6'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'TUESDAY' AND t.start_time = '16:00:00' AND t.end_time = '17:15:00');

-- Wednesday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '08:00:00', '09:15:00', 'Period 1'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '08:00:00' AND t.end_time = '09:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '09:30:00', '10:45:00', 'Period 2'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '09:30:00' AND t.end_time = '10:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '11:00:00', '12:15:00', 'Period 3'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '11:00:00' AND t.end_time = '12:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '13:00:00', '14:15:00', 'Period 4'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '13:00:00' AND t.end_time = '14:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '14:30:00', '15:45:00', 'Period 5'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '14:30:00' AND t.end_time = '15:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'WEDNESDAY', '16:00:00', '17:15:00', 'Period 6'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'WEDNESDAY' AND t.start_time = '16:00:00' AND t.end_time = '17:15:00');

-- Thursday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '08:00:00', '09:15:00', 'Period 1'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '08:00:00' AND t.end_time = '09:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '09:30:00', '10:45:00', 'Period 2'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '09:30:00' AND t.end_time = '10:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '11:00:00', '12:15:00', 'Period 3'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '11:00:00' AND t.end_time = '12:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '13:00:00', '14:15:00', 'Period 4'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '13:00:00' AND t.end_time = '14:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '14:30:00', '15:45:00', 'Period 5'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '14:30:00' AND t.end_time = '15:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'THURSDAY', '16:00:00', '17:15:00', 'Period 6'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'THURSDAY' AND t.start_time = '16:00:00' AND t.end_time = '17:15:00');

-- Friday time slots
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'FRIDAY', '08:00:00', '09:15:00', 'Period 1'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'FRIDAY' AND t.start_time = '08:00:00' AND t.end_time = '09:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'FRIDAY', '09:30:00', '10:45:00', 'Period 2'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'FRIDAY' AND t.start_time = '09:30:00' AND t.end_time = '10:45:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'FRIDAY', '11:00:00', '12:15:00', 'Period 3'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'FRIDAY' AND t.start_time = '11:00:00' AND t.end_time = '12:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'FRIDAY', '13:00:00', '14:15:00', 'Period 4'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'FRIDAY' AND t.start_time = '13:00:00' AND t.end_time = '14:15:00');
INSERT INTO time_slots (day_of_week, start_time, end_time, label)
SELECT 'FRIDAY', '14:30:00', '15:45:00', 'Period 5'
WHERE NOT EXISTS (SELECT 1 FROM time_slots t WHERE t.day_of_week = 'FRIDAY' AND t.start_time = '14:30:00' AND t.end_time = '15:45:00');
