package com.example.data.model

enum class TransactionType {
    BAKI_SALE,   // বিক্রি বাকি (Credit sale given to customer)
    REPAYMENT,   // জমা / পরিশোধ (Payment received from customer)
    CASH_SALE    // নগদ বিক্রি (Cash sale)
}
