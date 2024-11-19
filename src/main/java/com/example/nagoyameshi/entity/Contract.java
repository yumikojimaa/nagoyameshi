package com.example.nagoyameshi.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Table(name = "contracts")
@Data
public class Contract {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;    
    
    @Column(name = "customer_id", unique=true)
    private String customerId;
    
    @Column(name = "subscription_id")
    private String subscriptionId;
    
    @Column(name = "created_at", insertable = false, updatable = false)
    private Timestamp createdAt;
    
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Timestamp updatedAt;
    
    public boolean isNormal() {
    	return (this.getSubscriptionId() == null);
    }
}
