package com.app.expiry_system.suggestion.dto;

import java.util.List;

public class SuggestedMenuResponse {

    private String menuName;
    private String description;
    private List<String> ingredientsRequired;
    private List<String> ingredientsInStock;
    private List<String> missingIngredients;
    private List<String> steps;
    private String priority;
    private String reason;

    public SuggestedMenuResponse() {
    }

    public SuggestedMenuResponse(String menuName, String description, List<String> ingredientsRequired,
                                 List<String> ingredientsInStock, List<String> missingIngredients,
                                 List<String> steps, String priority, String reason) {
        this.menuName = menuName;
        this.description = description;
        this.ingredientsRequired = ingredientsRequired;
        this.ingredientsInStock = ingredientsInStock;
        this.missingIngredients = missingIngredients;
        this.steps = steps;
        this.priority = priority;
        this.reason = reason;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getIngredientsRequired() {
        return ingredientsRequired;
    }

    public void setIngredientsRequired(List<String> ingredientsRequired) {
        this.ingredientsRequired = ingredientsRequired;
    }

    public List<String> getIngredientsInStock() {
        return ingredientsInStock;
    }

    public void setIngredientsInStock(List<String> ingredientsInStock) {
        this.ingredientsInStock = ingredientsInStock;
    }

    public List<String> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<String> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }

    public List<String> getSteps() {
        return steps;
    }

    public void setSteps(List<String> steps) {
        this.steps = steps;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
