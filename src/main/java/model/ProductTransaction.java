package model;

import java.time.LocalDateTime;


public class ProductTransaction {
    private int id;
    private int productId;
    private int quantity;
    private LocalDateTime timestamp;
    private String type;

    public ProductTransaction() {
    }


    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ProductTransaction{" +
                "id=" + id +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", timestamp=" + timestamp +
                ", type='" + type + '\'' +
                '}';
    }


}
