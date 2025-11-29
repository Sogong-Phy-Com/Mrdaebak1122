# Contributing to Mr. Dinner Service

Thank you for your interest in contributing to Mr. Dinner Service! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 8 or higher
- Git
- Basic understanding of Object-Oriented Programming
- Familiarity with Domain-Driven Design concepts (helpful but not required)

### Development Environment Setup

1. **Fork and Clone the Repository**
   ```bash
   git clone https://github.com/yourusername/mr-dinner-service.git
   cd mr-dinner-service
   ```

2. **Verify Java Installation**
   ```bash
   java -version
   javac -version
   ```

3. **Build the Project**
   ```bash
   # Windows
   build.bat
   
   # Or manually
   javac -d . -source 8 -target 8 com\mrdinner\domain\common\*.java
   # ... (compile all packages)
   ```

## 📋 Contribution Guidelines

### Code Style
- Follow Java naming conventions
- Use meaningful variable and method names
- Add Javadoc comments for public methods
- Keep methods focused and single-purpose
- Use proper indentation (4 spaces)

### Architecture Principles
- **Domain-Driven Design**: Keep business logic in domain objects
- **Single Responsibility**: Each class should have one reason to change
- **Open/Closed Principle**: Open for extension, closed for modification
- **Dependency Inversion**: Depend on abstractions, not concretions

### Testing
- Test your changes thoroughly
- Add unit tests for new functionality
- Ensure existing tests still pass
- Test edge cases and error conditions

## 🎯 Areas for Contribution

### High Priority
- **Unit Tests**: Add comprehensive test coverage
- **Error Handling**: Improve error messages and exception handling
- **Documentation**: Enhance code comments and documentation
- **Performance**: Optimize algorithms and data structures

### Medium Priority
- **New Features**: Add new dinner types or payment methods
- **UI Integration**: Create a simple GUI or web interface
- **Database Integration**: Add persistence layer
- **Configuration**: Make system parameters configurable

### Low Priority
- **Logging**: Add proper logging framework
- **Monitoring**: Add metrics and monitoring
- **Internationalization**: Support multiple languages
- **Mobile App**: Create mobile companion app

## 🔄 Development Workflow

### 1. Create a Feature Branch
```bash
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes
- Write clean, readable code
- Add appropriate comments
- Test your changes
- Update documentation if needed

### 3. Commit Your Changes
```bash
git add .
git commit -m "Add: Brief description of your changes"
```

### 4. Push and Create Pull Request
```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub.

## 📝 Commit Message Guidelines

Use the following format for commit messages:

```
Type: Brief description

Detailed description of changes (if needed)

- Bullet point 1
- Bullet point 2
```

**Types:**
- `Add:` New features
- `Fix:` Bug fixes
- `Update:` Changes to existing features
- `Remove:` Removal of features
- `Refactor:` Code refactoring
- `Docs:` Documentation changes
- `Test:` Test additions or changes

**Examples:**
```
Add: Support for Apple Pay payment method

Implements Apple Pay integration with proper validation
and error handling for mobile payments.

- Added PaymentMethod.APPLE_PAY enum
- Updated PaymentService to handle Apple Pay
- Added validation for Apple Pay transactions
```

## 🧪 Testing Guidelines

### Unit Testing
- Test all public methods
- Test edge cases and error conditions
- Use meaningful test names
- Keep tests simple and focused

### Integration Testing
- Test service interactions
- Test complete workflows
- Test with sample data

### Manual Testing
- Run the main application
- Verify all features work as expected
- Test with different input scenarios

## 📚 Documentation

### Code Documentation
- Add Javadoc for all public methods
- Include parameter descriptions
- Document return values
- Add usage examples where helpful

### README Updates
- Update README.md for new features
- Add installation instructions for new dependencies
- Update sample code and output

### API Documentation
- Document new public APIs
- Provide usage examples
- Explain business rules and constraints

## 🐛 Bug Reports

When reporting bugs, please include:

1. **Description**: Clear description of the bug
2. **Steps to Reproduce**: Detailed steps to reproduce the issue
3. **Expected Behavior**: What should happen
4. **Actual Behavior**: What actually happens
5. **Environment**: Java version, OS, etc.
6. **Screenshots/Logs**: If applicable

## 💡 Feature Requests

When requesting features, please include:

1. **Description**: Clear description of the feature
2. **Use Case**: Why this feature would be useful
3. **Proposed Solution**: Your ideas for implementation
4. **Alternatives**: Other approaches you've considered

## 🔍 Code Review Process

### For Contributors
- Address all review comments
- Make requested changes
- Respond to feedback constructively
- Ask questions if anything is unclear

### For Reviewers
- Be constructive and respectful
- Focus on code quality and correctness
- Explain reasoning behind suggestions
- Approve when ready

## 📞 Getting Help

- **GitHub Issues**: For bugs and feature requests
- **Discussions**: For questions and general discussion
- **Email**: For private or sensitive matters

## 🎉 Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- GitHub contributor statistics

## 📄 License

By contributing to this project, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to Mr. Dinner Service! 🍽️
