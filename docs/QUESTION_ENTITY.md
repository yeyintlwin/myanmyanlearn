# Question Entity & QuestionOption Entity Documentation

## Overview

The **Question** and **QuestionOption** entities represent the assessment system in Myan Myan Learn. They provide a structured way to store, retrieve, and manage quiz/test questions with multiple choice options.

## Question Entity

### Purpose

Represents a single assessment question that can have multiple answer options and metadata.

### Fields

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key, auto-generated |
| `courseId` | String | Yes | Reference to the course (e.g., "jken") |
| `questionNumber` | Integer | No | Sequential question number within the assessment |
| `questionContent` | String (LONGTEXT) | No | The question text (supports Markdown/LaTeX) |
| `questionType` | String | No | Type of question (default: "multiple_choice") |
| `marks` | Float | No | Points value for this question (default: 1.0f) |
| `difficultyLevel` | String | Yes | Difficulty rating (e.g., "EASY", "MEDIUM", "HARD") |
| `explanation` | String (LONGTEXT) | Yes | Post-submission explanation or solution |
| `chapterId` | String | Yes | Reference to the chapter/content |
| `isActive` | Boolean | No | Whether the question is active (default: true) |
| `options` | List<QuestionOption> | Yes | Associated answer options (lazy-loaded) |
| `correctOptionIndex` | Integer | Yes | 0-based index of the correct answer in options list |

### Constructors

#### Default Constructor

```java
public Question()
```

Used by JPA for entity instantiation.

#### Primary Constructor

```java
public Question(String courseId, Integer questionNumber, String questionContent, Integer correctOptionIndex)
```

Used by `QuestionDataSource` and assessment creation logic. Auto-initializes:

- `questionType` = "multiple_choice"
- `marks` = 1.0f
- `isActive` = true

### Relationships

- **One-to-Many**: `options` → `QuestionOption` (cascade all, fetch eager, orphan removal)
  - Each question can have multiple options
  - Options are automatically deleted when question is deleted

### Database Mapping

```sql
CREATE TABLE questions (
    question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id VARCHAR(255),
    question_number INT NOT NULL,
    question_content LONGTEXT NOT NULL,
    question_type VARCHAR(255) NOT NULL,
    marks FLOAT NOT NULL,
    difficulty_level VARCHAR(255),
    explanation LONGTEXT,
    chapter_id VARCHAR(255),
    is_active BOOLEAN NOT NULL,
    correct_option_index INT
);
```

---

## QuestionOption Entity

### Purpose

Represents a single answer choice for a multiple-choice question.

### Fields

| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `id` | Long | No | Primary key, auto-generated |
| `question` | Question | No | Reference to parent question (lazy-loaded) |
| `optionIndex` | Integer | No | 0-based position in the options list |
| `optionContent` | String (LONGTEXT) | No | The text/content of this option |
| `isCorrect` | Boolean | No | Whether this option is the correct answer (default: false) |

### Constructors

#### Default Constructor

```java
public QuestionOption()
```

Used by JPA for entity instantiation.

#### Primary Constructor

```java
public QuestionOption(Integer optionIndex, String optionContent)
```

Used by `QuestionDataSource`. Auto-initializes:

- `isCorrect` = false

### Relationships

- **Many-to-One**: `question` → `Question` (lazy-loaded, cannot be null)
  - Each option belongs to exactly one question

### Database Mapping

```sql
CREATE TABLE question_options (
    option_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_index INT NOT NULL,
    option_content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    FOREIGN KEY (question_id) REFERENCES questions(question_id)
);
```

---

## Usage Examples

### Creating a Question with Options (via QuestionDataSource)

```java
// Method 1: Using sample questions
List<Question> questions = QuestionDataSource.getSampleQuestions();
Question q1 = questions.get(0); // Grammar question

// Method 2: Creating custom question
Question customQuestion = QuestionDataSource.createCustomQuestion(
    "jken",           // courseId
    "chapter-1",      // chapterId
    1,                // questionNumber
    "What is 2 + 2?", // questionContent
    1,                // correctOptionIndex (Option B)
    "EASY",           // difficulty
    2.0f,             // marks
    "3",              // optionTexts[0]
    "4",              // optionTexts[1] <- correct
    "5"               // optionTexts[2]
);
```

### Accessing Question Data

```java
Question question = repository.findById(1L).orElseThrow();

// Get basic info
String content = question.getQuestionContent();
int marks = question.getMarks();
String difficulty = question.getDifficultyLevel();

// Get options
List<QuestionOption> options = question.getOptions();
QuestionOption correctOption = options.get(question.getCorrectOptionIndex());

// Check if option is correct
if (correctOption.getIsCorrect()) {
    System.out.println("Correct answer: " + correctOption.getOptionContent());
}
```

### In Assessment Processing

```java
@PostMapping("/assessment/submit")
public String submitAssessment(AssessmentSubmission submission, Model model) {
    List<Long> selectedAnswers = submission.getSelectedAnswers(); // [1, 0, 2]
    List<Question> questions = questionRepository.findByChapterId(chapterId);
    
    int score = 0;
    for (int i = 0; i < questions.size(); i++) {
        Question q = questions.get(i);
        int userAnswer = selectedAnswers.get(i);
        
        if (userAnswer == q.getCorrectOptionIndex()) {
            score += q.getMarks();
        }
    }
    
    model.addAttribute("score", score);
    return "assessment-result";
}
```

---

## Repository Methods

See `QuestionRepository` for available queries:

```java
// Find questions by chapter
List<Question> questions = repository.findByChapterId("chapter-1");

// Find active questions only
List<Question> activeQuestions = repository.findByIsActiveTrue();

// Find by difficulty
List<Question> hardQuestions = repository.findByDifficultyLevel("HARD");

// Count questions
long total = repository.countByChapterId("chapter-1");
long active = repository.countActiveQuestionsByChapterId("chapter-1");
```

---

## Design Decisions

1. **Float marks, not Integer**: Allows fractional scoring (e.g., partial credit: 0.5 marks)
2. **correctOptionIndex field**: Denormalizes the relationship for quick lookups without loading all options
3. **Eager fetch on options**: Assessment screen needs all options immediately
4. **LaTeX/Markdown support**: `questionContent` and `optionContent` use LONGTEXT to support rich formatting (rendered client-side)
5. **Lazy-loaded question in option**: Prevents unnecessary parent loading when iterating options
6. **isActive flag**: Allows soft-delete or question deactivation without data loss

---

## Related Classes

- **QuestionRepository**: Data access layer for Question queries
- **QuestionDataSource**: Test data provider and factory for creating questions
- **AssessmentController**: Presentation layer consuming question data
- **QuestionTest / QuestionDataSourceTest**: Unit tests validating entity behavior
