package model;

import java.time.LocalDateTime;
import java.util.Objects;

public class ProductTransaction {
    private int id;
    private int productId;
    private int quantity;
    private LocalDateTime timestamp;
    private String type;

    public ProductTransaction() {
    }

    public ProductTransaction(int productId, int quantity, LocalDateTime timestamp, String type) {
        this.productId = productId;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.type = type;
    }

    public ProductTransaction(int id, int productId, int quantity, LocalDateTime timestamp, String type) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductTransaction)) return false;
        ProductTransaction that = (ProductTransaction) o;
        return id == that.id && productId == that.productId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, productId);
    }
}
