# WeatherApp

## Overview
WeatherApp is a JavaFX application that provides real-time weather information based on user queries. It connects to a weather server to fetch data and displays it in a user-friendly interface. The application also stores search queries in a database for future reference.

## Features
- Real-time weather updates for any city.
- Search history stored in a database.
- User-friendly JavaFX interface.
- Responsive design with CSS styling.

## Project Structure
```
WeatherApp
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── WeatherApp
│   │   │       ├── Client.java
│   │   │       ├── db
│   │   │       │   ├── DatabaseManager.java
│   │   │       │   ├── SearchQuery.java
│   │   │       │   └── SearchQueryDAO.java
│   │   │       └── util
│   │   │           └── DateUtils.java
│   │   └── resources
│   │       ├── WeatherApp
│   │       │   └── weather-styles.css
│   │       └── db
│   │           └── init.sql
│   └── test
│       └── java
│           └── WeatherApp
│               └── DatabaseManagerTest.java
└── README.md
```

## Setup Instructions
1. **Clone the repository**:
   ```
   git clone <repository-url>
   cd WeatherApp
   ```

2. **Build the project**:
   Ensure you have Maven installed. Run the following command to build the project:
   ```
   mvn clean install
   ```

3. **Database Setup**:
   - Ensure you have a compatible database (e.g., MySQL, SQLite) set up.
   - Run the SQL script located in `src/main/resources/db/init.sql` to create the necessary tables.

4. **Run the Application**:
   You can run the application using the following command:
   ```
   mvn javafx:run
   ```

## Usage
- Upon launching the application, you can enter a city name to fetch the current weather.
- The application will display the weather information along with an icon representing the weather condition.
- You can also view your search history, which is stored in the database.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.