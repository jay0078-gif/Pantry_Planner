package com.main.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity @Table(name="ingredient", uniqueConstraints=@UniqueConstraint(columnNames="name"))
@Getter @Setter
public class Ingredient {
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
@Column(nullable=false, length=120)
private String name;
@PrePersist @PreUpdate
void normalize() { if (name != null) name = name.trim().toLowerCase(); }

public Ingredient() {
	super();
	// TODO Auto-generated constructor stub
}

public Ingredient(Long id, String name) {
	super();
	this.id = id;
	this.name = name;
}

public Long getId() {
	return id;
}

public void setId(Long id) {
	this.id = id;
}

public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

@Override
public String toString() {
	return "Ingredient [id=" + id + ", name=" + name + "]";
}




}
