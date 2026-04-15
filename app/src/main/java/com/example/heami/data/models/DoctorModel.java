package com.example.heami.data.models;

import java.util.ArrayList;
import java.util.List;

public class DoctorModel {
    private String doctor_id;
    private String category_id;
    private double min_price;
    private boolean is_online;
    private String full_name;
    private String location;
    private String avatar_url;
    private double rating_avg;
    private int review_count;
    private List<String> specialization;
    private String degree;
    private int experience_years;
    private int total_sessions;
    private String bio;

    public DoctorModel() {
        this.specialization = new ArrayList<>();
    }

    public DoctorModel(String doctor_id, String category_id, double min_price, boolean is_online, String full_name, String location, String avatar_url, double rating_avg, int review_count, List<String> specialization, String degree, int experience_years, int total_sessions, String bio) {
        this.doctor_id = doctor_id;
        this.category_id = category_id;
        this.min_price = min_price;
        this.is_online = is_online;
        this.full_name = full_name;
        this.location = location;
        this.avatar_url = avatar_url;
        this.rating_avg = rating_avg;
        this.review_count = review_count;
        this.specialization = specialization != null ? specialization : new ArrayList<>();
        this.degree = degree;
        this.experience_years = experience_years;
        this.total_sessions = total_sessions;
        this.bio = bio;
    }

    public String getDoctor_id() {
        return doctor_id;
    }

    public void setDoctor_id(String doctor_id) {
        this.doctor_id = doctor_id;
    }

    public String getCategory_id() {
        return category_id;
    }

    public void setCategory_id(String category_id) {
        this.category_id = category_id;
    }

    public double getMin_price() {
        return min_price;
    }

    public void setMin_price(double min_price) {
        this.min_price = min_price;
    }

    public boolean isIs_online() {
        return is_online;
    }

    public void setIs_online(boolean is_online) {
        this.is_online = is_online;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public double getRating_avg() {
        return rating_avg;
    }

    public void setRating_avg(double rating_avg) {
        this.rating_avg = rating_avg;
    }

    public int getReview_count() {
        return review_count;
    }

    public void setReview_count(int review_count) {
        this.review_count = review_count;
    }

    public List<String> getSpecialization() {
        return specialization;
    }

    public void setSpecialization(List<String> specialization) {
        this.specialization = specialization;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public int getExperience_years() {
        return experience_years;
    }

    public void setExperience_years(int experience_years) {
        this.experience_years = experience_years;
    }

    public int getTotal_sessions() {
        return total_sessions;
    }

    public void setTotal_sessions(int total_sessions) {
        this.total_sessions = total_sessions;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
