package agh.gai;

import java.util.Objects;

class Book {
    private String title;
    private Integer price;
    private Integer shippingCost;
    private boolean offered;

    public Book(String title, Integer price, Integer shippingCost) {
        this.title = title;
        this.price = price;
        this.shippingCost = shippingCost;
        this.offered = false;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(Integer shippingCost) {
        this.shippingCost = shippingCost;
    }

    public Integer getTotalPrice() {
        return shippingCost + price;
    }

    public boolean getOffered() {
        return offered;
    }

    public void setOffered(boolean isOffered) {
        this.offered = isOffered;
    }

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        Book book = (Book) object;
        return title.equals(book.title) &&
                price.equals(book.price) &&
                shippingCost.equals(book.shippingCost);
    }

    public int hashCode() {
        return Objects.hash(super.hashCode(), title, price, shippingCost);
    }
}