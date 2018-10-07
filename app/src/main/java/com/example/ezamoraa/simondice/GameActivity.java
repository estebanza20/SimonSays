package com.example.ezamoraa.simondice;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    private List<ImageView> characters;
    private Map<ImageView, Integer> characters_icon;
    private SparseIntArray icons_sound;

    private MediaPlayer mp, theme_mp;

    private static final Integer ACTIVE_ALPHA = 255;
    private static final Integer INACTIVE_ALPHA = 100;

    private static final Float THEME_VOLUME_H = 0.3f;
    private static final Float THEME_VOLUME_L = 0.05f;

    private static final Integer MAX_HIGH_SCORES_NUM = 5;

    private GameStateMachine game_fsm;

    public enum GameState {
        STOP_STATE,
        GAME_START_STATE,
        CPU_TURN_STATE,
        PLAYER_TURN_STATE,
    }

    private class GameStateMachine {
        // FSM State
        public GameState state;

        // CPU Sequence
        private List<ImageView> cpu_sequence;
        private Iterator<ImageView> cpu_seq_it;
        private ImageView cpu_seq_iv;

        // Player Sequence
        private List<ImageView> player_sequence;
        private ImageView player_seq_iv;
        private Integer current_score;

        // Game Start Sequence
        private List<ImageView> start_sequence;
        private List<Integer> start_sounds;
        private Integer start_it;

        GameStateMachine() {
            // Initial game state
            reset();
        }

        private void reset() {
            state = GameState.STOP_STATE;
            updateStartGameButton();

            cpu_sequence = new ArrayList<>();
            player_sequence = new ArrayList<>();
            updatePlayerCurrentScore();
        }

        // Game opening sequence
        // ----------------------------------------------------------------------------------------
        private void startGameOpeningSequence() {
            if (state == GameState.STOP_STATE) {
                state = GameState.GAME_START_STATE;
                start_sequence = Arrays.asList(
                        (ImageView) findViewById(R.id.ssbb_three),
                        (ImageView) findViewById(R.id.ssbb_two),
                        (ImageView) findViewById(R.id.ssbb_one),
                        (ImageView) findViewById(R.id.ssbb_go)
                );
                start_sounds = Arrays.asList(
                        R.raw.narrator_three,
                        R.raw.narrator_two,
                        R.raw.narrator_one,
                        R.raw.narrator_go
                );

                start_it = 0;
                theme_mp.setVolume(THEME_VOLUME_L, THEME_VOLUME_L);

                startGameOpeningStep();
            }
        }

        private void startGameOpeningStep() {
            start_sequence.get(start_it).setVisibility(View.VISIBLE);

            mp = MediaPlayer.create(GameActivity.this, start_sounds.get(start_it));
            mp.setLooping(false);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer m) {
                    m.release();
                    start_sequence.get(start_it).setVisibility(View.INVISIBLE);
                    if (++start_it < start_sequence.size()) {
                        startGameOpeningStep();
                    } else {
                        theme_mp.setVolume(THEME_VOLUME_H, THEME_VOLUME_H);
                        startCpuSequence();
                    }
                }
            });
            mp.start();
        }

        // CPU sequence
        // ----------------------------------------------------------------------------------------
        private void startCpuSequence() {
            if (state == GameState.GAME_START_STATE ||
                    state == GameState.PLAYER_TURN_STATE) {
                state = GameState.CPU_TURN_STATE;
                updateStartGameButton();
                updateCpuSequence();
                // Start iterating over CPU sequence
                cpu_seq_it = cpu_sequence.iterator();
                startCpuStep();
            }
        }

        private void updateCpuSequence() {
            Random random = new Random();
            Integer rand_pos = random.nextInt(characters.size());
            cpu_sequence.add(characters.get(rand_pos));
        }

        private void startCpuStep() {
            if (cpu_seq_it.hasNext()) {
                cpu_seq_iv = cpu_seq_it.next();
                cpu_seq_iv.setImageAlpha(ACTIVE_ALPHA);
                Integer sound = icons_sound.get(characters_icon.get(cpu_seq_iv));
                mp = MediaPlayer.create(GameActivity.this, sound);
                mp.setLooping(false);

                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer m) {
                        m.release();
                        cpu_seq_iv.setImageAlpha(INACTIVE_ALPHA);
                        startCpuStep();
                    }
                });
                mp.start();
            } else {
                startPlayerSequence();
            }
        }

        // Player sequence
        // ----------------------------------------------------------------------------------------
        private void startPlayerSequence() {
            if (state == GameState.CPU_TURN_STATE) {
                state = GameState.PLAYER_TURN_STATE;
                updateStartGameButton();
                player_sequence = new ArrayList<>();
            }
        }

        private void updatePlayerSequence(ImageView iv) {
            if (state == GameState.PLAYER_TURN_STATE) {
                player_sequence.add(iv);

                // If player looses, start Game Over sequence
                if (!playerSequenceIsValid()) startGameOverSequence();
            }
        }

        private void startPlayerStep(ImageView iv) {
            if (state == GameState.PLAYER_TURN_STATE) {
                // Force stop previous player step
                if (mp != null) mp.release();
                if (player_seq_iv != null) {
                    player_seq_iv.setImageAlpha(INACTIVE_ALPHA);
                }

                // Prepare current player step
                player_seq_iv = iv;
                player_seq_iv.setImageAlpha(ACTIVE_ALPHA);

                Integer sound = icons_sound.get(characters_icon.get(player_seq_iv));
                mp = MediaPlayer.create(GameActivity.this, sound);
                mp.setLooping(false);
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer m) {
                        m.release();
                        player_seq_iv.setImageAlpha(INACTIVE_ALPHA);
                        if (playerSequenceIsComplete() && player_sequence.size() != 0) {
                            updatePlayerCurrentScore();
                            startCpuSequence();
                        }
                    }
                });
                mp.start();
            }
        }

        private void startGameOverSequence() {
            // Show final score and Game Over message
            String text = "GAME OVER\nFinal Score: " + String.valueOf(current_score);
            Toast toast = Toast.makeText(GameActivity.this, text, Toast.LENGTH_LONG);
            toast.show();

            // Play failure sound
            MediaPlayer game_over_mp = MediaPlayer.create(
                    GameActivity.this, R.raw.narrator_failure
            );
            game_over_mp.setLooping(false);
            game_over_mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer m) { m.release(); }
            });
            game_over_mp.start();

            // If score is high-score, store it
            updatePlayerHighScores();

            // Reset FSM
            reset();
        }

        private void updatePlayerCurrentScore() {
            current_score = player_sequence.size();
            TextView tv = findViewById(R.id.score_val);
            tv.setText(String.valueOf(current_score));
        }

        private void updatePlayerHighScores() {
            SharedPreferences hs_prefs = getSharedPreferences(
                    getResources().getString(R.string.hs_prefs), 0
            );

            List<Integer> high_scores = new ArrayList<>();

            // Read current high-scores
            Integer hs_num = hs_prefs.getInt(getResources().getString(R.string.hs_num), 0);
            for (int i=0; i<hs_num; i++) {
                high_scores.add(hs_prefs.getInt(String.valueOf(i),0));
            }

            // Check if player score is new high-score
            if (high_scores.size() == 0 || current_score >= Collections.min(high_scores)) {
                high_scores.add(current_score);
                Collections.sort(high_scores, Collections.reverseOrder());

                // Update high-scores
                SharedPreferences.Editor editor = hs_prefs.edit();

                hs_num = Math.min(high_scores.size(), MAX_HIGH_SCORES_NUM);
                editor.remove(getResources().getString(R.string.hs_num));
                editor.putInt(getResources().getString(R.string.hs_num), hs_num);

                for(int i=0; i<hs_num; i++) {
                    editor.remove(String.valueOf(i));
                    editor.putInt(String.valueOf(i), high_scores.get(i));
                }
                editor.apply();
            }
        }

        private Boolean playerSequenceIsValid() {
            for (int i = 0; i < player_sequence.size() && i < cpu_sequence.size(); i++) {
                if (player_sequence.get(i) != cpu_sequence.get(i)) return false;
            }
            return true;
        }

        private Boolean playerSequenceIsComplete() {
            return playerSequenceIsValid() && (player_sequence.size() == cpu_sequence.size());
        }

        private void updateStartGameButton() {
            String text;
            Integer bg_color, text_color;
            Button start_button = findViewById(R.id.start_game);

            switch(state) {
                case CPU_TURN_STATE:
                    text = getResources().getString(R.string.cpu);
                    bg_color = getResources().getColor(R.color.md_blue_700);
                    text_color = getResources().getColor(R.color.md_text_white);
                    break;
                case PLAYER_TURN_STATE:
                    text = getResources().getString(R.string.player);
                    bg_color = getResources().getColor(R.color.md_red_700);
                    text_color = getResources().getColor(R.color.md_text_white);
                    break;
                default:
                    text = getResources().getString(R.string.start);
                    bg_color = getResources().getColor(R.color.md_grey_600);
                    text_color = getResources().getColor(R.color.md_text_white);
            }

            start_button.setText(text);
            start_button.setBackgroundColor(bg_color);
            start_button.setTextColor(text_color);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // List of characters ImageView ids
        List<Integer> char_ids = Arrays.asList(
                R.id.character1,
                R.id.character2,
                R.id.character3,
                R.id.character4,
                R.id.character5,
                R.id.character6
        );

        characters = new ArrayList<>();
        for (Integer char_id : char_ids) {
            characters.add((ImageView)this.findViewById(char_id));
        }

        // List of characters icons
        List<Integer> icons = Arrays.asList(
                R.drawable.bowser_icon,
                R.drawable.captain_falcon_icon,
                R.drawable.fox_icon,
                R.drawable.falco_icon,
                R.drawable.ganondorf_icon,
                R.drawable.ice_climbers_icon,
                R.drawable.ike_icon,
                R.drawable.jigglypuff_icon,
                R.drawable.king_dedede_icon,
                R.drawable.kirby_icon,
                R.drawable.link_icon,
                R.drawable.lucario_icon,
                R.drawable.lucas_icon,
                R.drawable.luigi_icon,
                R.drawable.mario_icon,
                R.drawable.marth_icon,
                R.drawable.meta_knight_icon,
                R.drawable.mrgame_watch_icon,
                R.drawable.ness_icon,
                R.drawable.olimar_icon,
                R.drawable.peach_icon,
                R.drawable.pit_icon,
                R.drawable.pokemon_trainer_icon,
                R.drawable.rob_icon,
                R.drawable.samus_icon,
                R.drawable.snake_icon,
                R.drawable.sonic_icon,
                R.drawable.wario_icon,
                R.drawable.wolf_icon,
                R.drawable.yoshi_icon,
                R.drawable.zelda_icon
        );

        // List of characters sounds
        List<Integer> sounds = Arrays.asList(
                R.raw.bowser,
                R.raw.captain_falcon,
                R.raw.fox,
                R.raw.falco,
                R.raw.ganondorf,
                R.raw.ice_climbers,
                R.raw.ike,
                R.raw.jigglypuff,
                R.raw.king_dedede,
                R.raw.kirby,
                R.raw.link,
                R.raw.lucario,
                R.raw.lucas,
                R.raw.luigi,
                R.raw.mario,
                R.raw.marth,
                R.raw.meta_knight,
                R.raw.mrgame_watch,
                R.raw.ness,
                R.raw.olimar,
                R.raw.peach,
                R.raw.pit,
                R.raw.pokemon_trainer,
                R.raw.rob,
                R.raw.samus,
                R.raw.snake,
                R.raw.sonic,
                R.raw.wario,
                R.raw.wolf,
                R.raw.yoshi,
                R.raw.zelda
        );

        // Create map between icons and sounds
        icons_sound = new SparseIntArray();

        for (int i=0; i<icons.size() && i<sounds.size(); i++) {
            icons_sound.put(icons.get(i), sounds.get(i));
        }

        // Create map between characters and random icons
        characters_icon = new HashMap<>();
        List<Integer> rand_icons = new ArrayList<>(icons);
        Collections.shuffle(rand_icons);

        for (int i=0; i<characters.size() && i<rand_icons.size(); i++) {
            ImageView iv = characters.get(i);
            Integer icon = rand_icons.get(i);
            iv.setImageAlpha(INACTIVE_ALPHA);
            iv.setImageResource(icon);
            characters_icon.put(iv, icon);
        }

        // Initialize game FSM
        game_fsm = new GameStateMachine();

        // Start game theme
        theme_mp = MediaPlayer.create(GameActivity.this, R.raw.ssbb_menu1);
        theme_mp.setVolume(THEME_VOLUME_H, THEME_VOLUME_H);
        theme_mp.setLooping(true);
        theme_mp.start();
    }

    @Override
    public void onDestroy() {
        if (mp != null) mp.release();
        if (theme_mp != null) theme_mp.release();

        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void startGameOnClickHandle(View view) {
        game_fsm.startGameOpeningSequence();
    }

    public void characterOnClickHandle(View view) {
        ImageView iv = (ImageView) view;
        if (!game_fsm.playerSequenceIsComplete()) {
            game_fsm.updatePlayerSequence(iv);
            game_fsm.startPlayerStep(iv);
        }
    }
}
