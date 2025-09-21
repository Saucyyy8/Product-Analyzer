# Smart-Product-Manager
# Smart Product Analyzer 🧠✨

![Java](https://img.shields.io/badge/Java-21-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-9cf)
![Selenium](https://img.shields.io/badge/Selenium-4.10.0-green.svg)

The **Smart Product Analyzer** accepts a product URL or a text description, automatically builds a targeted Amazon search, and scrapes the top results. It then analyzes this information, distinguishes the pros and cons, and recommends the best available product.

The **Smart Product Analyzer** is a sophisticated Spring Boot application that leverages AI to transform natural language queries into detailed e-commerce product analyses. It can analyze products directly from a URL or intelligently search for a product based on a description, scrape the results, and deliver a comprehensive summary including pros, cons, a final verdict, and a rating.

This project demonstrates a powerful integration of web scraping with Large Language Models (LLMs) to create a smart, automated product intelligence tool.


https://github.com/user-attachments/assets/df39d72c-dc71-441c-b1c4-e207f626b3bf


---

## 🚀 Core Features

* **Dual-Mode Analysis**: Analyze a product either by providing a direct **URL** or a natural language **text description** (e.g., "a good mechanical keyboard under 5000").
* **AI-Powered Search**: Utilizes an AI model (via Ollama and Spring AI) to parse text descriptions into structured search criteria (keywords, brand, price range).
* **Automated Web Scraping**: Employs Selenium WebDriver to scrape Amazon product pages for names, prices, and customer reviews, with robust anti-detection configurations.
* **Intelligent Review Summarization**: Gathers product reviews and uses an AI prompt to generate a concise list of **Pros**, **Cons**, a final **Verdict**, and a calculated **Rating** out of 10.
* **RESTful API**: Provides clean, easy-to-use endpoints for initiating product analysis.
* **Resilient & Extensible**: Features custom exception handling for scraping and AI service failures, making the application robust and easy to debug.

---

## ⚙️ How It Works

The application has two primary workflows, both accessible via the API.

#### 1. Analyze by URL

This is the direct approach. You provide an Amazon product URL, and the service scrapes and analyzes it.


```
1. POST /analyze/url with an Amazon URL.
2. `AmazonScraperService` scrapes the product's name, price, and reviews.
3. `AiService` summarizes the reviews into pros, cons, a verdict, and a rating.
4. The final `Product` object is returned as JSON.
```

#### 2. Analyze by Description (The Smart Search)

This is the advanced, AI-driven workflow. You provide a text query, and the application finds and analyzes the best matching product.


```
1. POST /analyze/description with a text query (e.g., "logitech mouse for gaming").
2. `ProductService` sends the query to `AiService`.
3. `AiService` uses the `searchQueryPrompt.txt` template to ask the LLM to extract keywords, brand, and price.
4. `ProductService` builds a precise Amazon search URL based on the AI's response.
5. `AmazonSearchPageScraper` scrapes all product links from the search results page.
6. `ProductService` iterates through the links, scrapes each one, and uses the AI-generated rating to identify the "best" product.
7. The best `Product` object is returned as JSON.
```

---

## 🛠️ Tech Stack

| Component             | Technology / Library                                                                |
| --------------------- | ----------------------------------------------------------------------------------- |
| **Backend Framework** | Spring Boot 3.5.4                                                                   |
| **Language** | Java 21                                                                             |
| **AI Integration** | Spring AI 1.0.0 (with Ollama)                                                       |
| **Web Scraping** | Selenium WebDriver 4.10.0, WebDriverManager 6.2.0                                   |
| **API** | Spring Web (REST Controllers)                                                       |
| **Utilities** | Lombok, Jackson                                                                     |
| **Build Tool** | Maven                                                                               |
| **Exceptions** | Custom exceptions (`ScrapingException`, `ProductNotFound`, etc.) |
| **Cache** | Caffeine |

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed:

* **Java Development Kit (JDK) 21** or later.
* **Apache Maven**.
* **Git**.
* **Ollama**: The application is configured to connect to a local Ollama instance. You must have Ollama running with a model downloaded.
    * [Download Ollama](https://ollama.com/)
    * Pull the required model:
        ```bash
        ollama pull llama3
        ```

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone [https://github.com/Saucyyy8/Smart-Product-Manager.git](https://github.com/Saucyyy8/Smart-Product-Manager.git)
cd Smart-Product-Manager
```

### 2. Configure the Application
The core configuration is in `src/main/resources/application.properties`. By default, it's set to connect to Ollama on `localhost:11434` and use the `llama3:latest` model.

```properties
# App name
spring.application.name=productAnalyzer
spring.ai.ollama.base-url=http://localhost:11434

# Specify the Ollama model to be used by the ChatClient.
spring.ai.ollama.chat.model=llama3:latest

# Optional: You can configure model-specific parameters like temperature.
spring.ai.ollama.chat.options.temperature=0.7

#Added cache

spring.cache.cache-names=Product
spring.cache.caffeine.spec=expireAfterAccess=60m

```

### 3. Run the Application
Make sure your Ollama application is running. Then, start the Spring Boot application using Maven:

```bash
mvn spring-boot:run
```
The application will start on `http://localhost:8080`.

---

## 📚 API Endpoints

The API is defined in `ProductController.java`.

### Health Check

Verifies that the service is running.

* **URL:** `/product/health`
* **Method:** `GET`
* **Success Response (200 OK):**
    ```
    Smart Product Analyzer Running
    ```

### Analyze Product (Generic)

A single endpoint that intelligently handles both URL and text description inputs.

* **URL:** `/product/analyze`
* **Method:** `POST`
* **Request Body (`ProductAnalysisRequest`):**
    * For URL Analysis:
        ```json
        {
          "input": "[https://www.amazon.in/dp/B08XYZ123](https://www.amazon.in/dp/B08XYZ123)",
           "type": "url"
        }
        ```
    * For Description Analysis:
        ```json
        {
          "input": "A quiet mechanical keyboard from logitech",
           "type": "description"
        }
        ```
* **Success Response (200 OK):**
    Returns a `Product` object.
    ```json
    {
        "name": "Logitech MX Mechanical Wireless Keyboard",
        "price": "14999 INR",
        "url": "[https://www.amazon.in/Logitech-Mechanical-Wireless-Keyboard-Graphite/dp/B07S92QBC2](https://www.amazon.in/Logitech-Mechanical-Wireless-Keyboard-Graphite/dp/B07S92QBC2)",
        "pros": [
            "Excellent build quality and feels premium.",
            "Typing experience is quiet and satisfying.",
            "Long battery life and reliable wireless connectivity."
        ],
        "cons": [
            "The price is on the higher side.",
            "Not ideal for gaming due to standard polling rates."
        ],
        "verdict": "A top-tier productivity keyboard that is worth the investment for professionals.",
        "rating": 8.75
    }
    ```
* **Error Response (500 Internal Server Error):**
    ```json
    {
        "timestamp: ": "2025-08-09T12:00:00.000Z",
        "HttpStatus :": 505,
        "error :": "Internal Server Error",
        "message :": "Exception message details..."
    }
    ```

---

## 🧠 AI Prompts

A key part of this project is how it instructs the AI model. The prompts are stored in the `src/main/resources/prompts/` directory.

### `searchQueryPrompt.txt`

This prompt instructs the LLM to act as a text processor and extract structured search criteria from a user's free-form text.

```
You are a text processing bot. Your task is to extract search criteria from a user's query and format it into a strict 5-line output.
...
User Query: {query}
```

### `productAnalyzer.txt`

This prompt takes a collection of scraped reviews and asks the LLM to summarize them into a structured analysis.

```
I have a product from Amazon with the following reviews:
{reviews}
Please analyze these reviews and provide the following information in this exact format:
PROS:- [list each pro...]
CONS:- [list each con...]
VERDICT:-[one-line verdict...]
RATING:-[rating out of 10...]
```

---

## 📝 License

This project is licensed under the MIT License.
