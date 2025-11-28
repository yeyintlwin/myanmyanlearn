# Question Implementation Guide

## Overview

This guide explains how to integrate the Question and QuestionOption entities into the Assessment workflow of Myan Myan Learn.

## Architecture

### Entity Relationship Diagram

```
Question (1) ──── (many) QuestionOption
  • id                    • id
  • courseId              • question (FK)
  • questionNumber        • optionIndex
  • questionContent       • optionContent
  • marks                 • isCorrect
  • correctOptionIndex
  • options (List)
```

## Current Implementation Status

✅ **Completed:**

- Question entity with all fields from assessment UI
- QuestionOption entity for answer choices
- QuestionRepository with custom query methods
- QuestionDataSource for test data generation
- Unit tests for both entities
- Database schema via Hibernate auto-DDL

❌ **Pending:**

- AssessmentService integration
- Question loading/retrieval in AssessmentController
- Answer submission and grading logic

## Integration Steps

### 1. AssessmentService (Service Layer)

Create `/src/main/java/com/barlarlar/myanmyanlearn/service/AssessmentService.java`:

```java
@Service
@Transactional
public class AssessmentService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    /**
     * Get all questions for an assessment by chapter
     */
    public List<Question> getQuestionsForChapter(String chapterId) {
        return questionRepository.findActiveQuestionsByChapterId(chapterId);
    }
    
    /**
     * Get a specific question with all options
     */
    public Question getQuestion(Long questionId) {
        return questionRepository.findById(questionId)
            .orElseThrow(() -> new RuntimeException("Question not found"));
    }
    
    /**
     * Grade an assessment submission
     */
    public AssessmentResult gradeAssessment(AssessmentSubmission submission) {
        List<Question> questions = questionRepository.findByChapterId(
            submission.getChapterId()
        );
        
        float totalScore = 0;
        List<QuestionResult> results = new ArrayList<>();
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            int userAnswer = submission.getAnswers().get(i);
            boolean isCorrect = userAnswer == q.getCorrectOptionIndex();
            
            if (isCorrect) {
                totalScore += q.getMarks();
            }
            
            results.add(new QuestionResult(
                q.getId(),
                userAnswer,
                q.getCorrectOptionIndex(),
                isCorrect,
                q.getExplanation()
            ));
        }
        
        return new AssessmentResult(totalScore, results);
    }
}
```

### 2. AssessmentController Modifications

Update `/src/main/java/com/barlarlar/myanmyanlearn/controller/AssessmentController.java`:

```java
@Controller
public class AssessmentController {
    
    @Autowired
    private AssessmentService assessmentService;
    
    @Autowired
    private CourseService courseService;
    
    @GetMapping("/assessment")
    public String showAssessment(
            @RequestParam String courseId,
            @RequestParam(defaultValue = "") String chapters,
            Model model) {
        
        // Parse selected chapters
        List<String> selectedChapters = Arrays.asList(chapters.split(","));
        
        // Load questions from all selected chapters
        List<Question> allQuestions = new ArrayList<>();
        for (String chapterId : selectedChapters) {
            allQuestions.addAll(
                assessmentService.getQuestionsForChapter(chapterId)
            );
        }
        
        // Prepare model
        Course course = courseService.findById(courseId);
        model.addAttribute("courseId", courseId);
        model.addAttribute("questions", allQuestions);
        model.addAttribute("totalQuestions", allQuestions.size());
        model.addAttribute("examTitle", course.getTitle());
        model.addAttribute("selectedChapters", selectedChapters);
        
        return "assessment";
    }
    
    @PostMapping("/assessment/submit")
    public String submitAssessment(
            @RequestBody AssessmentSubmission submission,
            Model model) {
        
        AssessmentResult result = assessmentService.gradeAssessment(submission);
        model.addAttribute("result", result);
        model.addAttribute("totalScore", result.getTotalScore());
        
        return "assessment-result";
    }
}
```

### 3. Update assessment.html Template

Modify `/src/main/resources/templates/assessment.html` to use dynamic question data:

```html
<!-- Repeat for each question -->
<th:block th:each="question : ${questions}">
    <section class="question-card bg-white/10 border border-white/10 rounded-xl p-4 sm:p-6" 
             data-mark="1" th:attr="data-mark=${question.marks}">
        <div class="flex items-center justify-between mb-3">
            <h2 class="text-white text-lg font-semibold flex items-center gap-2">
                <span class="inline-flex items-center justify-center w-6 h-6 rounded bg-white/10 border border-white/10 text-xs" 
                      th:text="${question.questionNumber}"></span>
                <span th:text="'Question ' + ${question.questionNumber}">Question 1</span>
            </h2>
            <div class="flex items-center gap-3">
                <span class="text-white/80 text-sm question-mark-label" 
                      th:text="${question.marks} + ' ' + (${question.marks == 1} ? 'mark' : 'marks')">1 mark</span>
            </div>
        </div>
        <div class="bg-amber-50 border border-slate-200 rounded-xl p-3 mb-3">
            <article class="prose max-w-none text-slate-800" 
                     th:utext="${question.questionContent}"></article>
        </div>
        <div class="space-y-2">
            <th:block th:each="option, stat : ${question.options}">
                <label class="flex items-center gap-3 text-white/90">
                    <input type="radio" 
                           th:name="'q' + ${question.id}" 
                           th:value="${stat.index}" 
                           class="w-4 h-4 accent-blue-500" />
                    <span th:text="${option.optionContent}">Option text</span>
                </label>
            </th:block>
        </div>
    </section>
</th:block>
```

### 4. DTOs for Assessment

Create `/src/main/java/com/barlarlar/myanmyanlearn/dto/AssessmentSubmission.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentSubmission {
    private String courseId;
    private String chapterId;
    private List<Integer> answers; // Index of selected option per question
    private Long submissionTime;
}
```

Create `/src/main/java/com/barlarlar/myanmyanlearn/dto/AssessmentResult.java`:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResult {
    private Float totalScore;
    private Float maxScore;
    private Float percentage;
    private List<QuestionResult> questionResults;
}
```

## Testing Workflow

### 1. Load Sample Questions

```java
@Test
public void testLoadSampleQuestions() {
    List<Question> questions = QuestionDataSource.getSampleQuestions();
    assertTrue(questions.size() > 0);
    
    // Verify first question
    Question q = questions.get(0);
    assertEquals(1, q.getQuestionNumber());
    assertEquals(3, q.getOptions().size());
}
```

### 2. Grade Submission

```java
@Test
public void testGradeAssessment() {
    AssessmentSubmission sub = new AssessmentSubmission();
    sub.setAnswers(Arrays.asList(0, 0, 0)); // All correct
    
    AssessmentResult result = assessmentService.gradeAssessment(sub);
    assertEquals(3.0f, result.getTotalScore());
}
```

## Database Seeding

To populate the database with sample questions on startup:

```java
@Component
public class QuestionDataSeeder implements CommandLineRunner {
    
    @Autowired
    private QuestionRepository repository;
    
    @Override
    public void run(String... args) throws Exception {
        if (repository.count() == 0) {
            List<Question> questions = QuestionDataSource.getSampleQuestions();
            repository.saveAll(questions);
            System.out.println("Seeded " + questions.size() + " sample questions");
        }
    }
}
```

## Markdown & LaTeX Support

Questions support client-side rendering of:

- **Markdown**: Headers, lists, code blocks
- **LaTeX**: Math equations via KaTeX (use `$...$` for inline, `$$...$$` for display)
- **HTML**: Basic HTML tags in question content

Example question:

```java
String content = "**Solve:** $2x + 3 = 9$\n\n" +
                 "Given the equation above, find the value of $x$.";
```

## Performance Considerations

1. **Eager fetch on options**: Options are fetched with question to avoid N+1 queries in assessment view
2. **Index on chapterId**: Add database index for quick chapter-based question retrieval
3. **Caching**: Consider caching assessment questions if they don't change frequently

## Migration Notes

If questions already exist in a legacy system:

1. Map legacy question format to Question entity
2. Create QuestionOption records for each legacy answer
3. Set `correctOptionIndex` based on legacy "correct answer" field
4. Verify all marks and difficulty levels are populated

## Future Enhancements

- [ ] Question bank/pool management
- [ ] Question versioning for historical tracking
- [ ] Question tagging and categorization
- [ ] Randomize question order per assessment
- [ ] Adaptive difficulty based on student performance
- [ ] Image/formula support in questions
- [ ] Timed assessments with countdown
