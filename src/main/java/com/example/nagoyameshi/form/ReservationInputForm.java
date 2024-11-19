package com.example.nagoyameshi.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReservationInputForm {
    @NotBlank(message = "予約日を指定してください。")
    private String fromReservationDate;    
    
    @NotNull(message = "時間を入力してください。")
    //@Min(value = 1, message = "時間を設定してください。")
    private String reservationTime; 
    
    @NotNull(message = "人数を入力してください。")
    @Min(value = 1, message = "人数は1人以上に設定してください。")
    private Integer numberOfPeople; 
    
   
}