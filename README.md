# Favs List

> Disclaimer: this project is vibe coded.

Favs List is an application for saving the things people like in organized personal or shared lists.

The main idea is simple: people often discover something they want to remember later, but the important details get lost. This application gives them a place to register those favorites with context, so the list is useful when they come back to it.

## Purpose

Favs List helps users create lists of things they enjoy, want to repeat, or want to recommend.

A favorite is more than just a name. Users should be able to save details that make the memory useful, such as:

- what the item was
- where they found it
- how much it cost
- how they felt about it
- any notes they want to keep

Example:

Someone goes out for ice cream and wants to remember:

- the flavor they ordered
- the shop they visited
- the price
- whether they liked it
- any extra comments for next time

Instead of saving that in random notes, they can keep it in a structured list.

## Main Idea

The application is intended to support:

- personal lists for individual users
- grouped favorites organized by topic or category
- group-based lists that can be created and maintained together

This makes the app useful for many situations, such as:

- favorite foods and drinks
- restaurants and cafes
- books, movies, and games
- travel spots
- products people want to buy again
- recommendations shared with friends, family, or teams

## Why This Project Exists

People already save favorites in many disconnected places: notes apps, chat messages, screenshots, bookmarks, or memory alone. Those approaches usually make it hard to compare options or remember the details that mattered.

Favs List is meant to solve that by giving users a dedicated space to keep:

- what they liked
- why they liked it
- where it came from
- whether they would choose it again

## Current Project Status

This repository currently contains the initial backend scaffold for the application using Spring Boot and Kotlin.

At the moment, the business features described above are the product direction for the application. The full domain model, APIs, persistence, and group-list behavior still need to be implemented.

## Technology Stack

Current project setup:

- Kotlin
- Spring Boot
- Maven
- Spring Web MVC
- Spring Data JPA
- Spring GraphQL

## Running the Project

Requirements:

- Java 24
- Maven 3.9+ or the included Maven Wrapper

Run locally with the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Initial Vision Summary

Favs List is being built to help people remember what they like, keep useful details about those favorites, and create lists alone or with groups.
