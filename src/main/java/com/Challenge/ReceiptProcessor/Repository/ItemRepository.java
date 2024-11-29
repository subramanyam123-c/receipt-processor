package com.Challenge.ReceiptProcessor.Repository;

import com.Challenge.ReceiptProcessor.Entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

}
