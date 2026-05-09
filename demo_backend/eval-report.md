# Project Assistant — Evaluation Report

- **Generated:** 2026-05-09T12:38:42.327242500Z
- **Prompt version:** `v1.0`
- **Model:** `gemini-2.5-flash`
- **Catalog size:** 20 items
- **Golden set size:** 25 cases

## Summary metrics

| Metric | Value |
|---|---|
| Mean precision | **0.761** |
| Mean recall | **0.879** |
| Mean F1 | **0.787** |
| Parse success rate | 11 / 25 (44.0%) |
| Total items returned | 46 |
| Hallucinations | 0 |
| Hallucination rate | 0.00% |
| Latency p50 | 0 ms |
| Latency p95 | 12307 ms |
| Latency max | 16611 ms |
| Total prompt tokens | 20785 |
| Total completion tokens | 7697 |

## Per-case results

| Case | Parsed | P | R | F1 | Hall. | Latency | Recommended |
|---|---|---|---|---|---|---|---|
| `weather_station_wifi` | ✓ | 1.00 | 1.00 | 1.00 | 0 | 12307 ms | ESP32 dev board ×1, BME280 sensor ×1, MicroSD card breakout ×1, 18650 battery pack ×1, LM2596 buck converter ×1, Breadboard (830 tie-points) ×1 |
| `soil_moisture_basic` | ✓ | 0.50 | 1.00 | 0.67 | 0 | 9005 ms | Arduino Uno R3 ×1, Breadboard (830 tie-points) ×1, 18650 battery pack ×1 |
| `led_clock` | ✓ | 1.00 | 0.67 | 0.80 | 0 | 7379 ms | ESP32 dev board ×1, SSD1306 OLED display ×1, NeoPixel ring (16 LED) ×1, Breadboard (830 tie-points) ×1 |
| `parking_sensor` | ✓ | 1.00 | 1.00 | 1.00 | 0 | 10237 ms | Arduino Uno R3 ×1, HC-SR04 ultrasonic distance sensor ×1, SSD1306 OLED display ×1, Breadboard (830 tie-points) ×1 |
| `motion_alarm` | ✓ | 1.00 | 1.00 | 1.00 | 0 | 8252 ms | Arduino Uno R3 ×1, PIR motion sensor ×1, Breadboard (830 tie-points) ×1 |
| `stepper_curtain` | ✓ | 0.40 | 0.67 | 0.50 | 0 | 16611 ms | ESP32 dev board ×1, NEMA 17 stepper motor ×1, A4988 stepper driver ×1, 18650 battery pack ×1, LM2596 buck converter ×1, Breadboard (830 tie-points) ×1 |
| `pet_feeder` | ✓ | 1.00 | 1.00 | 1.00 | 0 | 10547 ms | Arduino Uno R3 ×1, SG90 micro servo ×1, Breadboard (830 tie-points) ×1 |
| `fall_detector` | ✓ | 0.67 | 0.67 | 0.67 | 0 | 7461 ms | ESP32 dev board ×1, MPU6050 IMU ×1, 18650 battery pack ×1, Breadboard (830 tie-points) ×1 |
| `image_classifier` | ✓ | 0.33 | 1.00 | 0.50 | 0 | 7165 ms | Raspberry Pi 4 ×1, SSD1306 OLED display ×1, Breadboard (830 tie-points) ×1 |
| `solar_outdoor_logger` | ✓ | 0.80 | 0.67 | 0.73 | 0 | 11570 ms | ESP32 dev board ×1, BME280 sensor ×1, MicroSD card breakout ×1, Solar panel 6V 1W ×1, 18650 battery pack ×1, LM2596 buck converter ×1, Breadboard (830 tie-points) ×1 |
| `smart_relay` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 10740 ms | (none) |
| `pan_tilt_camera_mount` | ✓ | 0.67 | 1.00 | 0.80 | 0 | 7412 ms | Arduino Uno R3 ×1, SG90 micro servo ×2, Breadboard (830 tie-points) ×1 |
| `obstacle_avoiding_robot` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `iot_thermometer` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `lcd_message_board` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `presence_lighting` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `rgb_desk_decoration` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `remote_logger_no_wifi` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `step_counter` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `garage_door_logger` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `robot_arm_two_joints` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `homelab_dashboard` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `beginner_button_led` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `outdoor_solar_neopixel` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |
| `distance_lcd_meter` | ✗ | 0.00 | 0.00 | 0.00 | 0 | 0 ms | (none) |

## Detailed errors

### `soil_moisture_basic`

- **Unjustified (false positives):** [18650 battery pack]

### `led_clock`

- **Missed (false negatives):** [Arduino Uno R3]

### `stepper_curtain`

- **Missed (false negatives):** [Arduino Uno R3]
- **Unjustified (false positives):** [LM2596 buck converter, Breadboard (830 tie-points), 18650 battery pack]

### `fall_detector`

- **Missed (false negatives):** [Arduino Uno R3]
- **Unjustified (false positives):** [Breadboard (830 tie-points)]

### `image_classifier`

- **Unjustified (false positives):** [SSD1306 OLED display, Breadboard (830 tie-points)]

### `solar_outdoor_logger`

- **Missed (false negatives):** [DHT22 sensor, Arduino Uno R3]
- **Unjustified (false positives):** [Breadboard (830 tie-points)]

### `smart_relay`

- **Parse failed:** Parse failed: Response is not valid JSON: Unexpected character ('{' (code 123)): was expecting double-quote to start property name

### `pan_tilt_camera_mount`

- **Unjustified (false positives):** [Breadboard (830 tie-points)]

### `obstacle_avoiding_robot`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 20.70617005s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "model": "gemini-2.5-flash",
              "location": "global"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}
]

### `iot_thermometer`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 20.523512914s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}
]

### `lcd_message_board`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 20.366205194s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}
]

### `presence_lighting`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 20.192941701s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}
]

### `rgb_desk_decoration`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 20.024879479s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "20s"
      }
    ]
  }
}
]

### `remote_logger_no_wifi`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.871688432s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `step_counter`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.702984445s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `garage_door_logger`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.540321521s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `robot_arm_two_joints`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.354990754s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `homelab_dashboard`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.202723925s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `beginner_button_led`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 19.051186105s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "19s"
      }
    ]
  }
}
]

### `outdoor_solar_neopixel`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 18.877092437s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}
]

### `distance_lcd_meter`

- **Parse failed:** LLM call failed: LLM provider returned HTTP 429 TOO_MANY_REQUESTS: [{
  "error": {
    "code": 429,
    "message": "You exceeded your current quota, please check your plan and billing details. For more information on this error, head to: https://ai.google.dev/gemini-api/docs/rate-limits. To monitor your current usage, head to: https://ai.dev/rate-limit. \n* Quota exceeded for metric: generativelanguage.googleapis.com/generate_content_free_tier_requests, limit: 5, model: gemini-2.5-flash\nPlease retry in 18.689785267s.",
    "status": "RESOURCE_EXHAUSTED",
    "details": [
      {
        "@type": "type.googleapis.com/google.rpc.Help",
        "links": [
          {
            "description": "Learn more about Gemini API quotas",
            "url": "https://ai.google.dev/gemini-api/docs/rate-limits"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.QuotaFailure",
        "violations": [
          {
            "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
            "quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier",
            "quotaDimensions": {
              "location": "global",
              "model": "gemini-2.5-flash"
            },
            "quotaValue": "5"
          }
        ]
      },
      {
        "@type": "type.googleapis.com/google.rpc.RetryInfo",
        "retryDelay": "18s"
      }
    ]
  }
}
]

## Methodology

- **Precision** = TP / (TP + FP). FP = returned items that were neither expected nor flagged as acceptable.
- **Recall** = TP / |expected|. Missing an expected item costs recall.
- **F1** = harmonic mean of precision and recall.
- **Hallucinations** = returned items whose name does not match any catalog entry. Hallucinations are a strict subset of false positives.
- **Latency** is end-to-end time including the LLM call (prompt assembly and parsing are negligible by comparison).
- **Tokens** are taken from the provider's `usage` block. Calls that don't report usage contribute 0.
- **Availability** is only annotated when the validator chain runs. ✓ = AVAILABLE, ⚠ = INSUFFICIENT_STOCK, ✗ = OUT_OF_STOCK.
- Each case has an `expectedItems` set (must appear) and an optional `acceptableItems` set (OK to appear, doesn't penalise precision).
- Cases where parsing fails contribute precision/recall/F1 = 0 to the means.
