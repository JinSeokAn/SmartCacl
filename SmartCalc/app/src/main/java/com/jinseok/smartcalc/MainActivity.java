package com.jinseok.smartcalc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText inputInvestment;
    private EditText inputTotalValue;
    private EditText inputPercentage;
    private EditText inputBaseValue;
    private EditText inputAdjustPercentage;
    private Spinner dropdownAdjustOperation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 기존 UI 연결
        inputTotalValue = findViewById(R.id.input_total_value);
        inputPercentage = findViewById(R.id.input_percentage);
        TextView textResultFirst = findViewById(R.id.text_result_first);

        // 새로운 UI 연결
        inputInvestment = findViewById(R.id.input_reference_value);
        inputBaseValue = findViewById(R.id.input_base_value);
        inputAdjustPercentage = findViewById(R.id.input_adjust_percentage);
        EditText inputChangedValue = findViewById(R.id.input_changed_value);
        TextView textResultSecond = findViewById(R.id.text_result_second);
        TextView textResultThird = findViewById(R.id.text_result_third);
        Button buttonAddScreenshot = findViewById(R.id.button_add_screenshot);

        // Spinner 연결
        dropdownAdjustOperation = findViewById(R.id.dropdown_adjust_operation);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.operation_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownAdjustOperation.setAdapter(adapter);

        dropdownAdjustOperation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                calculateThirdSection();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // TextWatcher를 통한 실시간 계산 로직
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 첫 번째 계산 섹션 (전체 값과 비율 계산)
                calculateFirstSection();

                // 두 번째 계산 섹션 (기준값과 변경값 계산)
                calculateSecondSection();
                // 세 번째 계산 섹션 (기준값에서 일정 비율로 증가/감소한 결과를 계산합니다.)
                calculateThirdSection();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        // TextWatcher를 EditText에 적용
        inputTotalValue.addTextChangedListener(textWatcher);
        inputPercentage.addTextChangedListener(textWatcher);
        inputBaseValue.addTextChangedListener(textWatcher);
        inputChangedValue.addTextChangedListener(textWatcher);
        inputAdjustPercentage.addTextChangedListener(textWatcher);

        // 초기화 버튼 이동 및 기능 추가
        findViewById(R.id.button_reset).setOnClickListener(v -> {
            inputTotalValue.setText("");
            inputPercentage.setText("");
            inputBaseValue.setText("");
            inputChangedValue.setText("");
            inputInvestment.setText("");
            inputAdjustPercentage.setText("");


            textResultFirst.setText("결과 값 : ");
            textResultSecond.setText("결과 값 : ");
            textResultThird.setText("결과 값 : ");
        });

        // 스크린샷 버튼 클릭 이벤트 - 갤러리 열기
        buttonAddScreenshot.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();

            if (selectedImageUri != null) {

                try {
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);

                    Bitmap enhancedBitmap = enhanceImageContrast(originalBitmap);
                    InputImage image = InputImage.fromBitmap(enhancedBitmap, 0);

                    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

                    recognizer.process(image)
                            .addOnSuccessListener(visionText -> {
                                String recognizedText = visionText.getText();
                                String[] lines = recognizedText.split("\n");

                                if (lines.length >= 11) {
                                    String[] fourthLineParts = lines[4].replaceAll(",", "").split("s", 2);
                                    if (fourthLineParts.length > 0) {
                                        String fourthLineData = fourthLineParts[0].trim();
                                        Log.d("MainActivity", "4번째 행 데이터: " + fourthLineData);
                                        inputBaseValue.setText(fourthLineData);
                                    }

                                    String eleventhLine = lines[11].replaceAll("[^0-9]", "").trim();
                                    inputInvestment.setText(eleventhLine);
                                    inputTotalValue.setText(eleventhLine);
                                    Toast.makeText(this, "11번째 행 값이 입력되었습니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "11번째 행 데이터를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("MainActivity", "텍스트 인식 실패", e);
                                Toast.makeText(this, "이미지에서 텍스트를 인식하지 못했습니다.", Toast.LENGTH_SHORT).show();
                            });

                } catch (Exception e) {
                    Log.e("MainActivity", "이미지 처리 실패", e);
                    Toast.makeText(this, "이미지에서 텍스트를 처리하는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap enhanceImageContrast(Bitmap original) {
        Bitmap enhancedBitmap = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());

        for (int x = 0; x < original.getWidth(); x++) {
            for (int y = 0; y < original.getHeight(); y++) {
                int pixel = original.getPixel(x, y);

                int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                red = enhanceColorValue(red);
                green = enhanceColorValue(green);
                blue = enhanceColorValue(blue);

                enhancedBitmap.setPixel(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return enhancedBitmap;
    }

    private int enhanceColorValue(int color) {
        color = (int) (color * 1.2);
        return Math.min(255, Math.max(0, color));
    }

    private void calculateFirstSection() {
        String totalText = inputTotalValue.getText().toString();
        String percentageText = inputPercentage.getText().toString();

        TextView textResultFirst = findViewById(R.id.text_result_first);
        if (!totalText.isEmpty() && !percentageText.isEmpty()) {
            try {
                double total = Double.parseDouble(totalText);
                double percentage = Double.parseDouble(percentageText);
                double result = (total * percentage) / 100;
                textResultFirst.setText(String.format("결과 : %.2f", result));
            } catch (NumberFormatException e) {
                textResultFirst.setText("올바른 숫자를 입력하세요.");
            }
        } else {
            textResultFirst.setText("결과 값 : ");
        }
    }


    private void calculateSecondSection() {
        String baseText = inputBaseValue.getText().toString();
        String changedText = ((EditText) findViewById(R.id.input_changed_value)).getText().toString();

        TextView textResultSecond = findViewById(R.id.text_result_second);
        if (!baseText.isEmpty() && !changedText.isEmpty()) {
            try {
                double base = Double.parseDouble(baseText);
                double changed = Double.parseDouble(changedText);
                double percentage = ((changed - base) / base) * 100;

                if (percentage > 0) {
                    textResultSecond.setText(String.format("결과 증가: %.2f%%", percentage));
                } else {
                    textResultSecond.setText(String.format("결과 감소: %.2f%%", Math.abs(percentage)));
                }
            } catch (NumberFormatException e) {
                textResultSecond.setText("올바른 숫자를 입력하세요.");
            }
        } else {
            textResultSecond.setText("결과 값 : ");
        }
    }

    private void calculateThirdSection() {
        String baseText = inputInvestment.getText().toString();
        String adjustText = inputAdjustPercentage.getText().toString();

        TextView textResultThird = findViewById(R.id.text_result_third);
        if (true) {
            try {
                double base = Double.parseDouble(baseText);
                double adjustPercentage = Double.parseDouble(adjustText);
                String selectedOperation = dropdownAdjustOperation.getSelectedItem().toString();

                double result;
                if ("증가".equals(selectedOperation)) {
                    result = base * (1 + (adjustPercentage / 100));
                    textResultThird.setText(String.format("결과 증가 후: %.2f", result));
                } else {
                    result = base * (1 - (adjustPercentage / 100));
                    textResultThird.setText(String.format("결과 감소 후: %.2f", result));
                }
            } catch (NumberFormatException e) {
                textResultThird.setText("올바른 숫자를 입력하세요.");
            }
        } else {
            textResultThird.setText("결과 값 : ");
        }
    }

}
