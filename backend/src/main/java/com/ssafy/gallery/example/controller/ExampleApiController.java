package com.ssafy.gallery.example.controller;

import com.ssafy.gallery.example.model.BasicResponseDto;
import com.ssafy.gallery.example.model.MemberJoinRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Tag(name = "Example", description = "Example API")
@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/example")
public class ExampleApiController {

    // Log4j Logger
    private static final Logger logger = LogManager.getLogger(ExampleApiController.class);

    @PostMapping("/test")
    @Operation(summary = "Example API Summary", description = "Your description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success",
                    content = {@Content(schema = @Schema(implementation = BasicResponseDto.class))}),
            @ApiResponse(responseCode = "404", description = "Not Found"),
    })
    public BasicResponseDto exampleAPI(
            //Query Parameter
            @Parameter(name = "paramValue", description = "Parameter Value", example = "3", required = true)
            @RequestParam final Long paramValue,

            //Request Body
            @RequestBody @Valid MemberJoinRequestDto requestBody
    ) {
        logger.info("request : " + requestBody);
        String s = String.format("ParamValue = %s, Request Email : %s", paramValue, requestBody.getEmail());
        BasicResponseDto response = new BasicResponseDto(true, "Example API Success", s);
        logger.info("response : " + response);
        return response;
    }


}