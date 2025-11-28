# Question Model and DataSource Implementation

## Overview

Created a comprehensive Question Model system for the Assessment Screen, including both entity classes for database persistence and model classes for non-persistent DTOs.

## Files Created

### 1. Entity Classes (Database Persistence)

#### `/src/main/java/com/barlarlar/myanmyanlearn/entity/Question.java`

- **Purpose**: JPA entity for storing questions in the database
- **Key Fields**:
  - `id` (Long): Auto-generated primary key
  - `courseId` (String): Identifies the course (e.g., "jken")
  - `questionNumber` (int): Sequential question number
  - `questionContent` (String): The question text/markdown (supports markdown and KaTeX)
  - `correctOptionIndex` (int): 0-based index of the correct answer
  - `marks` (float): Points allocated to this question (score)
  - `difficultyLevel` (String): Question difficulty (EASY, MEDIUM, HARD)
  - `chapterId` (String): Optional chapter identifier for organization
  - `explanation` (String): Explanation for the correct answer
  - `options` (List<QuestionOption>): Collection of answer options
  - `isActive` (boolean): Marks if question is active (default: true)
  - `questionType` (String): Type of question (default: "multiple_choice")

**Database Table**: `questions`

---

#### `/src/main/java/com/barlarlar/myanmyanlearn/entity/QuestionOption.java`

- **Purpose**: JPA entity for storing answer options in the database
- **Key Fields**:
  - `id` (Long): Auto-generated primary key
  - `question` (Question): Foreign key relationship to parent question
  - `optionIndex` (int): 0-based index of the option (0, 1, 2, etc.)
  - `optionContent` (String): The option text/content
  - `isCorrect` (boolean): Flag indicating if this is the correct answer

**Database Table**: `question_options`
**Relationship**: One-to-Many with Question (cascade delete enabled)

---

### 2. Model Classes (Non-persistent DTOs)

#### `/src/main/java/com/barlarlar/myanmyanlearn/model/Question.java`

- Non-persistent POJO for transferring question data
- Same structure as entity but without JPA annotations
- Used for service layer operations and data transfer

#### `/src/main/java/com/barlarlar/myanmyanlearn/model/QuestionOption.java`

- Non-persistent POJO for transferring option data
- Same structure as entity but without JPA annotations
- Simpler structure without foreign key relationship

---

### 3. Data Source Class (Testing)

#### `/src/main/java/com/barlarlar/myanmyanlearn/datasource/QuestionDataSource.java`

- **Purpose**: Provides sample questions for testing and development
- **Key Methods**:

##### `getSampleQuestions()`: Returns 3 sample questions

```
Q1: English Grammar (EASY) - "Which sentence uses the correct form of 'to be'?"
    - Correct answer: "I am happy."
    - Marks: 1.0

Q2: JavaScript (MEDIUM) - "What does function add(a, b) return?"
    - Correct answer: "The sum of `a` and `b`."
    - Marks: 1.0

Q3: Algebra (EASY) - "Solve for x: 2x + 3 = 9"
    - Correct answer: "x = 3"
    - Marks: 1.0
```

##### `createQuestion()`: Factory method for creating questions

```java
Question q = QuestionDataSource.createQuestion(
    courseId, questionNumber, questionContent,
    correctOptionIndex, marks, options
);
```

##### `createCustomQuestion()`: Convenience method with automatic option marking

```java
Question q = QuestionDataSource.createCustomQuestion(
    courseId, chapterId, questionNumber, questionContent,
    correctOptionIndex, difficultyLevel, marks,
    "Option A", "Option B", "Option C"
);
```

---

## Integration with Assessment Screen

The Question models are designed to work with the existing Assessment UI template (`assessment.html`):

- **Question Content**: Supports Markdown with KaTeX math notation
- **Multiple Choice**: Fixed to 3 options per question (configurable)
- **Scoring**: Each question has individual marks
- **Difficulty**: Three levels (EASY, MEDIUM, HARD)
- **Explanation**: Post-submission explanation for learning

---

## Test Coverage

All tests passing (20/20):

### QuestionDataSourceTest (9 tests)

- ✅ getSampleQuestions()
- ✅ testSampleQuestion1/2/3 (content, options, marks validation)
- ✅ testCreateQuestion()
- ✅ testCreateCustomQuestion() (with correct option marking)
- ✅ testQuestionOptionCreation/Correctness()
- ✅ testSampleQuestionsOptions()

### QuestionTest (11 tests)

- ✅ testQuestionCreation()
- ✅ testQuestionMarks/Difficulty/Options/Active/ChapterId/Explanation/Type()
- ✅ testQuestionToString()

---

## Key Design Patterns

### 1. **Dual Model Approach**

- **Entity**: For database operations (JPA managed)
- **Model**: For DTO/transfer operations (lightweight)

### 2. **Factory Pattern**

- QuestionDataSource provides static factory methods
- Simplifies question creation with various configurations

### 3. **Markdown Support**

- Questions support full Markdown syntax
- Integrates with KaTeX for mathematical equations
- Supports code highlighting via highlight.js

### 4. **Flexible Option Marking**

- `createOptionsWithCorrectIndex()`: Marks any option as correct
- Useful for testing different correct answer positions

---

## Usage Examples

### Basic Usage (with Sample Questions)

```java
List<Question> questions = QuestionDataSource.getSampleQuestions();
// Returns 3 questions ready for assessment
```

### Custom Question Creation

```java
Question q = QuestionDataSource.createCustomQuestion(
    "jken",                    // courseId
    "chapter_2",               // chapterId
    1,                         // questionNumber
    "What is 2 + 2?",         // questionContent
    2,                         // correctOptionIndex (third option)
    "EASY",                    // difficultyLevel
    2.5f,                      // marks
    "3", "4", "5"             // options
);
```

### Assessment Integration

```java
@GetMapping("/assessment")
public String assessment(Model model) {
    List<Question> questions = QuestionDataSource.getSampleQuestions();
    model.addAttribute("questions", questions);
    model.addAttribute("examTitle", "JKen Assessment");
    return "assessment";
}
```

---

## Database Schema

### Questions Table

```sql
CREATE TABLE questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id VARCHAR(255) NOT NULL,
    question_number INT NOT NULL,
    question_content LONGTEXT NOT NULL,
    correct_option_index INT NOT NULL,
    marks FLOAT NOT NULL,
    difficulty_level VARCHAR(255),
    chapter_id VARCHAR(255),
    explanation LONGTEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    question_type VARCHAR(255) NOT NULL DEFAULT 'multiple_choice'
);
```

### Question Options Table

```sql
CREATE TABLE question_options (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    option_index INT NOT NULL,
    option_content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);
```

---

## Next Steps

1. **Create Repository interfaces** for database operations
   - `QuestionRepository` (extends JpaRepository)
   - `QuestionOptionRepository` (extends JpaRepository)

2. **Create Service layer** for business logic
   - `QuestionService` for retrieving/managing questions
   - Integration with existing assessment workflow

3. **Update Controllers** to wire in the Question services
   - Modify `AssessmentController` to load real questions
   - Handle question submission and scoring

4. **Create Assessment Service** for:
   - Grading submitted answers
   - Calculating scores
   - Storing assessment results

5. **Add More Sample Questions** for different courses/chapters
   - Extend QuestionDataSource with more test data
   - Support bulk question loading from JSON

---

## File Summary

| File | Type | Purpose |
|------|------|---------|
| `entity/Question.java` | Entity | Database persistence for questions |
| `entity/QuestionOption.java` | Entity | Database persistence for options |
| `model/Question.java` | Model | Non-persistent DTO for questions |
| `model/QuestionOption.java` | Model | Non-persistent DTO for options |
| `datasource/QuestionDataSource.java` | Utility | Sample data and factory methods |
