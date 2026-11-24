package com.bankify.transactionservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_types", uniqueConstraints = {
    @UniqueConstraint(columnNames = "typeName")
})
public class TransactionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long typeId;

    @Column(nullable = false, unique = true)
    private String typeName;

    @Column
    private String description;
    
    @Column(nullable = false)
    private Boolean active = true;

    // Default constructor
    public TransactionType() {
        this.active = true;
    }

    // Parameterized constructor
    public TransactionType(String typeName, String description) {
        this.typeName = typeName;
        this.description = description;
        this.active = true;
    }

    // Getters and Setters
    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(Long typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
}
