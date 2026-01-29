package org.campusscheduler.generator;

/**
 * University archetypes based on research analysis of Canadian institutions.
 *
 * These archetypes are derived from morphological analysis of Canada's top universities,
 * including University of Toronto, UBC, McGill, University of Alberta, University of Waterloo,
 * and Lakehead University. Each archetype represents a distinct campus morphology with
 * characteristic density ratios.
 *
 * Research Source: "Architectonics of Academe: A Comprehensive Analysis of Institutional
 * Morphology and Access Protocols for Procedural Generation" (2026)
 *
 * @see <a href="https://www.utoronto.ca/about-u-of-t/quick-facts">University of Toronto Facts</a>
 * @see <a href="https://www.lakeheadu.ca/research-and-innovation/about/facts-figures">Lakehead Facts</a>
 */
public enum UniversityArchetype {

    /**
     * High-density urban campus with vertical architecture.
     * Based on: University of Toronto (St. George), McGill University, University of Waterloo
     *
     * Characteristics:
     * - Dense building utilization (500 students per building)
     * - Multi-story, multi-department buildings
     * - High course-per-building ratio (80 courses per building)
     * - Urban integration with porous campus boundaries
     * - Typical enrollment: 40,000 - 80,000 students
     *
     * Research notes:
     * - S/C ratio (studentsPerCourse) of 7.0 means less course variety per student
     * - High density means fewer buildings serve more students
     */
    METROPOLIS(
        "Urban Titan",
        "High-density urban campus with vertical architecture and intense utilization",
        500,    // studentsPerBuilding (U of T: 583, McGill: 393, Waterloo: 410)
        80,     // coursesPerBuilding (U of T: 83, McGill: 80, Waterloo: 75)
        7.0,    // studentsPerCourse (S/C ratio - U of T: 7.0, higher = less variety)
        40000,  // minStudents
        80000,  // maxStudents
        0.70,   // academicBuildingRatio (70% of buildings are academic)
        3       // avgFloorsPerBuilding
    ),

    /**
     * Sprawling research campus with parkland feel.
     * Based on: University of British Columbia, University of Alberta
     *
     * Characteristics:
     * - Low building density (200 students per building)
     * - Many specialized, single-purpose structures
     * - Lower course-per-building ratio (30 courses per building)
     * - Expansive land use, isolated campus feel
     * - Typical enrollment: 30,000 - 60,000 students
     *
     * Research notes:
     * - S/C ratio of 5.5 indicates moderate course variety
     * - Many specialized research buildings dilute density
     */
    CAMPUS_SPRAWL(
        "Research Park",
        "Expansive campus with parkland feel and specialized research facilities",
        200,    // studentsPerBuilding (UBC: 125, Alberta: 267)
        30,     // coursesPerBuilding (UBC: 22, Alberta: 53)
        5.5,    // studentsPerCourse (S/C ratio - UBC: 5.6, Alberta: 5.0)
        30000,  // minStudents
        60000,  // maxStudents
        0.50,   // academicBuildingRatio (more research/support buildings)
        2       // avgFloorsPerBuilding
    ),

    /**
     * Community-focused comprehensive institution.
     * Based on: Lakehead University
     *
     * Characteristics:
     * - Moderate building density (200 students per building)
     * - Efficient use of space, interconnected buildings
     * - Moderate course-per-building ratio (55 courses per building)
     * - High accessibility and personalized attention
     * - Typical enrollment: 5,000 - 15,000 students
     * - Often features tunnel/pedway systems for climate adaptation
     *
     * Research notes:
     * - S/C ratio of 3.6 is the "Golden Number" for student experience
     * - Lower ratio = more course variety per student = better personalization
     */
    COMMUNITY(
        "Community Hub",
        "Accessible, interconnected campus with personalized attention and efficient space use",
        200,    // studentsPerBuilding (Lakehead: 202)
        55,     // coursesPerBuilding (Lakehead: 56)
        3.6,    // studentsPerCourse (S/C ratio - Lakehead: 3.6, best student experience)
        5000,   // minStudents
        15000,  // maxStudents
        0.60,   // academicBuildingRatio
        2       // avgFloorsPerBuilding
    );

    private final String displayName;
    private final String description;
    private final int studentsPerBuilding;
    private final int coursesPerBuilding;
    private final double studentsPerCourse; // S/C ratio from research (higher = less variety)
    private final int minStudents;
    private final int maxStudents;
    private final double academicBuildingRatio;
    private final int avgFloorsPerBuilding;

    UniversityArchetype(
            String displayName,
            String description,
            int studentsPerBuilding,
            int coursesPerBuilding,
            double studentsPerCourse,
            int minStudents,
            int maxStudents,
            double academicBuildingRatio,
            int avgFloorsPerBuilding) {
        this.displayName = displayName;
        this.description = description;
        this.studentsPerBuilding = studentsPerBuilding;
        this.coursesPerBuilding = coursesPerBuilding;
        this.studentsPerCourse = studentsPerCourse;
        this.minStudents = minStudents;
        this.maxStudents = maxStudents;
        this.academicBuildingRatio = academicBuildingRatio;
        this.avgFloorsPerBuilding = avgFloorsPerBuilding;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getStudentsPerBuilding() {
        return studentsPerBuilding;
    }

    public int getCoursesPerBuilding() {
        return coursesPerBuilding;
    }

    /**
     * Returns the S/C ratio (students per course offering).
     * Higher values mean less course variety per student.
     * This is NOT the number of courses a student takes (that's typically 5).
     *
     * @return students per course offering (S/C ratio from research)
     */
    public double getStudentsPerCourse() {
        return studentsPerCourse;
    }

    /**
     * @deprecated Use {@link #getStudentsPerCourse()} instead. This method exists
     * for backwards compatibility but the semantics were incorrect.
     */
    @Deprecated
    public double getCoursesPerStudent() {
        return studentsPerCourse; // Return same value for compatibility
    }

    public int getMinStudents() {
        return minStudents;
    }

    public int getMaxStudents() {
        return maxStudents;
    }

    public double getAcademicBuildingRatio() {
        return academicBuildingRatio;
    }

    public int getAvgFloorsPerBuilding() {
        return avgFloorsPerBuilding;
    }

    /**
     * Calculates the number of buildings needed for a given student population.
     *
     * @param studentPopulation the total student population
     * @return the calculated number of buildings
     */
    public int calculateBuildings(int studentPopulation) {
        return Math.max(1, studentPopulation / studentsPerBuilding);
    }

    /**
     * Calculates the number of academic buildings (for course scheduling).
     *
     * @param totalBuildings the total number of buildings
     * @return the number of academic buildings
     */
    public int calculateAcademicBuildings(int totalBuildings) {
        return Math.max(1, (int) (totalBuildings * academicBuildingRatio));
    }

    /**
     * Calculates the number of courses based on academic buildings.
     *
     * @param academicBuildings the number of academic buildings
     * @return the calculated number of courses
     */
    public int calculateCourses(int academicBuildings) {
        return academicBuildings * coursesPerBuilding;
    }

    /**
     * Calculates instructors needed based on courses (assuming ~3 courses per instructor).
     *
     * @param courses the number of courses
     * @return the calculated number of instructors
     */
    public int calculateInstructors(int courses) {
        return Math.max(1, courses / 3);
    }

    /**
     * Validates if a student population is within the recommended range for this archetype.
     *
     * @param studentPopulation the student population to validate
     * @return true if within recommended range
     */
    public boolean isRecommendedPopulation(int studentPopulation) {
        return studentPopulation >= minStudents && studentPopulation <= maxStudents;
    }
}
