package naren.ragu.chip8emujavafx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.Random;

public class Chip8 {

    Random random;
    boolean emulate = true;

    boolean drawFlag;

    char opcode;

    char[] memory;

    char[] V; // register file

    char I; // index counter
    char pc; // program counter

    // 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
    // 0x050-0x0A0 - Used for the built in 4x5 pixel font set (0-F)
    // 0x200-0xFFF - Program ROM and work RAM

    char[] gfx;

    char delay_timer;
    char sound_timer;

    char[] stack;
    char sp;

    char[] key;

    char[] fontset;


    public Chip8(){
        random = new Random();
        memory = new char[4096];
        V = new char[16];
        gfx = new char[64 * 32];
        stack = new char[16];
        key = new char[16];

        fontset = new char[]{
          0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
          0x20, 0x60, 0x20, 0x20, 0x70, // 1
          0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
          0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
          0x90, 0x90, 0xF0, 0x10, 0x10, // 4
          0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
          0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
          0xF0, 0x10, 0x20, 0x40, 0x40, // 7
          0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
          0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
          0xF0, 0x90, 0xF0, 0x90, 0x90, // A
          0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
          0xF0, 0x80, 0x80, 0x80, 0xF0, // C
          0xE0, 0x90, 0x90, 0x90, 0xE0, // D
          0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
          0xF0, 0x80, 0xF0, 0x80, 0x80  // F
        };

        initialize();
    }

    void incPC(){
        pc += 2;
    }

    void dontIncPC(){
        pc -= 2;
    }

    void initialize(){
        // Initialize registers and memory once

        pc = 0x200; // PC starts at 0x200
        opcode = 0; // Reset opcode
        I = 0;      // Reset index register
        sp = 0;     // Reset stack pointer

        // Clear display
        gfx = new char[64 * 32];
        // Clear stack
        stack = new char[16];
        // Clear registers V0-VF
        V = new char[16];
        // Clear memory
        memory = new char[4096];

        // Load fontset
        for(int i = 0; i < 80; ++i){
            memory[i + 0x50] = fontset[i];
        }

        // Reset timers
        delay_timer = 0;
        sound_timer = 0;

        memory[0x1FF] = 0x1;
    }

    void emulateCycle(){

        if(!emulate){
            return;
        }

        // Fetch Opcode
        opcode = (char)((memory[pc] << 8) | memory[pc + 1]);
        // Decode Opcode

        // Get instruction
        switch(opcode & 0xF000){
            case 0x0000:
                switch(opcode & 0x00FF){
                    case 0x00E0: // 0x00E0 : Clear screen
                        gfx = new char[64 * 32];
                        break;
                    case 0x00EE: // 0x00EE : Return from subroutine
                        sp--;
                        pc = stack[sp];
                        break;
                    case 0x0000:{
                        emulate = false;
                        dontIncPC();
                        break;
                    }
                    default:
                        System.out.println("Invalid 0 Opcode " + Integer.toHexString(opcode & 0xFFFF));
                        dontIncPC();
                        break;
                }
                break;
            case 0x1000: // 0x1XXX : Jump to address at XXX
                pc = (char)(opcode & 0x0FFF);
                dontIncPC();
                break;
            case 0x2000: // 0x2XXX : Call subroutine at XXX
                stack[sp] = pc;
                ++sp;
                pc = (char) (opcode & 0x0FFF);
                dontIncPC();
                break;
            case 0x3000: { // 0x3XNN : Skip next instruction if RX == NN
                char register = (char)((opcode & 0x0F00) >>> 8);
                char value = (char) (opcode & 0x00FF);
                if (V[register] == value) {
                    incPC();
                }
                break;
            }
            case 0x4000: { // 0x4XNN : Skip next instruction if RX != NN
                char register = (char) ((opcode & 0x0F00) >>> 8);
                char value = (char) (opcode & 0x00FF);
                if (V[register] != value) {
                    incPC();
                }
                break;
            }
            case 0x5000: { // 0x5XY0 : Skip next instruction if RX == RY
                char register1 = (char) ((opcode & 0x0F00) >>> 8);
                char register2 = (char) ((opcode & 0x00F0) >>> 4);
                if (V[register1] == V[register2]) {
                    incPC();
                }
                break;
            }
            case 0x6000: { // 0x6XNN : Set RX to NN
                char register = (char) ((opcode & 0x0F00) >>> 8);
                char value = (char) (opcode & 0x00FF);
                V[register] = value;
                break;
            }
            case 0x7000: { // 0x7XNN : Add NN to RX
                char register = (char) ((opcode & 0x0F00) >>> 8);
                char value = (char) (opcode & 0x00FF);
                V[register] = (char) ((V[register] + value) & 0xFF);
                break;
            }
            case 0x8000: { // Bitwise operations
                char X = (char)((opcode & 0x0F00) >>> 8);
                char Y = (char)((opcode & 0x00F0) >>> 4);
                switch (opcode & 0x000F) {
                    case 0x0000: { // 8XY0 : set RX to RY
                        V[X] = V[Y];
                        break;
                    }
                    case 0x0001: { // 8XY1 : set RX to bitwise RX or RY
                        V[X] |= V[Y];
                        break;
                    }
                    case 0x0002: { // 8XY2 : set RX to bitwise RX and RY
                        V[X] &= V[Y];
                        break;
                    }
                    case 0x0003: { // 8XY3 : set RX to bitwise RX xor RY
                        V[X] ^= V[Y];
                        break;
                    }
                    case 0x0004: { // 8XY4 : adds RY to RX, RF is set to 1 if overflow, 0 if not
                        char value = (char) (V[X] + V[Y]);
                        V[X] = (char) (value & 0xFF);
                        V[0xF] = (char) (value >> 8);
                        break;
                    }
                    case 0x0005: { // 8XY5 : sets RX to RX - RY, RF is 0 when underflow, 1 if not (when RX >= RY)
                        char noCarry = 0;
                        if(V[X] >= V[Y]){
                            noCarry = 1; // no carry
                        }

                        V[X] = (char) ((V[X] - V[Y]) & 0xFF);
                        V[0xF] = noCarry;

                        break;
                    }
                    case 0x0006: { // 8XY6 : store least significant bit of RX in RF, shift RX to right by 1
                        int value = V[X];

                        V[X] = (char) (V[X] >>> 1);
                        V[0xF] = (value & 0x1) == 1  ?  (char)1 :  (char)0;

                        break;
                    }
                    case 0x0007: { // 8XY7 : sets RX to RY - RX, RF is 0 when underflow, 1 if not (when RY >= RX)
                        char noCarry = 0;
                        if(V[Y] >= V[X]){
                            noCarry = 1; // no carry
                        }

                        V[X] = (char) ((V[Y] - V[X]) & 0xFF);

                        V[0xF] = noCarry;

                        break;
                    }
                    case 0x000E: { // 8XYE : store most significant bit of RX in RF, shift RX to left by 1

                        char value = (char)(V[X] << 1);
                        V[X] = (char)((byte)(value & 0xFF));

                        V[0xF] = (char)((value & 0x100) >> 8);

                        break;
                    }

                    default:
                        System.out.println("Invalid 8 Opcode " + Integer.toHexString(opcode & 0xFFFF));
                        dontIncPC();
                        break;
                }
                break;
            }
            case 0x9000: { // 0x9XY0 : Skip next instruction if RX != RY
                short register1 = (short) ((opcode & 0x0F00) >>> 8);
                short register2 = (short) ((opcode & 0x00F0) >>> 4);
                if (V[register1] != V[register2]) {
                    incPC();
                }
                break;
            }
            case 0xA000: // ANNN : set I to address NNN
                I = (char)(opcode & 0x0FFF);
                break;
            case 0xB000: // BNNN : jump to address NNN + R0
                short address = (short) (opcode & 0x0FFF);
                pc = (char)(V[0x0] + address);
                dontIncPC();
                break;
            case 0xC000: // CXNN : Set RX to result of bitwise NN and rand(0 to 255)
                short register1 = (short) ((opcode & 0x0F00) >>> 8);
                short num = (short) (opcode & 0x00FF);
                V[register1] = (char) (num & random.nextInt(256));
                break;
            case 0xD000: { // DXYN : draw sprite at coordinate XY with width 8 and height N
                short x = (short) V[(opcode & 0x0F00) >> 8];
                short y = (short) V[(opcode & 0x00F0) >> 4];
                short height = (short) (opcode & 0x000F);
                short pixel;

                V[0xF] = 0;
                for (int yline = 0; yline < height; yline++) {
                    pixel = (short) memory[I + yline];
                    for(int xline = 0; xline < 8; xline++) {
                        if((pixel & (0x80 >> xline)) != 0) {
                            if(gfx[(((x + xline)%64) + (((y + yline)%32) * 64))] == 1) {
                                V[0xF] = 1;
                            }
                            gfx[x + xline + ((y + yline) * 64)] ^= 1;
                        }
                    }
                }

                drawFlag = true;
                break;
            }
            case 0xE000: { // key operations
                switch(opcode & 0x00FF){
                    case 0x009E: { // EX9E : Skip next instruction if key == RX
                        short register = (short) ((opcode & 0x0F00) >> 8);
                        if(key[V[register]] != 0){
                            incPC();
                        }
                        break;
                    }
                    case 0x00A1: { // EXA1 : Skip next instruction if key != RX
                        short register = (short) ((opcode & 0x0F00) >> 8);
                        if(key[V[register]] == 0){
                            incPC();
                        }
                        break;
                    }
                    default:
                        System.out.println("Invalid E Opcode " + Integer.toHexString(opcode & 0xFFFF));
                        dontIncPC();
                        break;
                }
                break;
            }
            case 0xF000:{
                short register = (short) ((opcode & 0x0F00) >> 8); // Get register # from 0x0X00 (used for all below instructions0

                switch (opcode & 0x00FF){
                    case 0x0007: { // FX07 : Store delay timer in RX
                        V[register] = delay_timer;
                        break;
                    }
                    case 0x000A: { // FX0A : A key press is awaited, and then stored in RX
                        // TODO fix? implementation
                        boolean keyPress = false;
                        for(int i = 0; i < 16; i++){
                            if(key[i] != 0){
                                V[register] = (char) i;
                                keyPress = true;
                            }
                        }
                        if(!keyPress){
                            return;
                        }
                        break;
                    }
                    case 0x0015: { // FX15 : Set delay timer to RX
                        delay_timer = V[register];
                        break;
                    }
                    case 0x0018: { // FX18 : Set sound timer to RX
                        sound_timer = V[register];
                        break;
                    }
                    case 0x001E: { // FX1E : Adds RX to I (RX flag is not changed)
                        I = (char)(I + V[register]);
                        break;
                    }
                    case 0x0029: { // FX29 : Sets I to the location of the sprite for the character in RX
                        I = (char)((0x50) + (5 * (short)V[register]));
                        break;
                    }
                    case 0x0033: { // FX33 : Stores binary coded decimal representation of RX at I, I+1, and I+2
                        memory[I]     = (char) (V[register] / 100);
                        memory[I + 1] = (char) ((V[register] % 100) / 10);
                        memory[I + 2] = (char) ((V[register] % 100) % 10);
                        break;
                    }
                    case 0x0055: { // FX55 : Stores from R0 - RX in memory, starting at address I. offset += 1 for each value (I itself doesn't change)
                        System.arraycopy(V, 0, memory, I, register + 1);
                        break;
                    }
                    case 0x0065: { // FX65 : Loads into R0 - RX from memory, starting at address I. offset += 1 for each value (I itself doesn't change)
                        System.arraycopy(memory, I, V, 0, register + 1);
                        break;
                    }
                    default:
                        System.out.println("Invalid F Opcode " + Integer.toHexString(opcode & 0xFFFF));
                        dontIncPC();
                        break;
                }
                break;
            }
            default:
                System.out.println("Invalid Opcode " + Integer.toHexString(opcode & 0xFFFF));
                dontIncPC();;
                break;
        }

        incPC();

        // Execute Opcode

        // Update timers

        if(delay_timer > 0){
            --delay_timer;
        }

        if(sound_timer > 0){
            if(sound_timer == 1){
                System.out.println("BEEP!");
            }
            --sound_timer;
        }
    }

    void loadGame(String name){
        try {
            Path path = Paths.get(name);
            byte[] contents = Files.readAllBytes(path);
            for (int i = 0; i < contents.length; i++){
                char data = (char)(0xFF & contents[i]);
                memory[i + 0x200] = data;
            }
        } catch (IOException e) {
            System.out.println("Game File Not Found: " + name);
        }
    }

    public char[] getGfx(){
        return gfx;
    }

    public void setKeys(){

    }

    public void printMem(){
        HexFormat hex = HexFormat.of();
        for(int i = 0; i < memory.length; i = i+16){
            StringBuilder line = new StringBuilder(hex.toHexDigits(i).toUpperCase() + " | ");

            for(int j = i; j < i+16; j++){
                line.append((hex.toHighHexDigit(0xFF & memory[j]) + "" + hex.toLowHexDigit(0xFF & memory[j])).toUpperCase()).append(" ");
            }

            System.out.println(line);
        }

    }

    public void printRegisters(){
        HexFormat hex = HexFormat.of();

        StringBuilder registers = new StringBuilder("Registers: ");
        for(int i = 0; i < V.length; i++){
            registers.append("R").append(i).append(": ");
            registers.append((hex.toHighHexDigit(0xFF & V[i]) + "" + hex.toLowHexDigit(0xFF & V[i])+ " | ").toUpperCase());
        }
        System.out.println(registers);

        String pcOut = "PC: " + (hex.toHighHexDigit(0xFF & pc) + "" + hex.toLowHexDigit(0xFF & pc)+ " ").toUpperCase();
        String iOut = "I: " + Integer.toHexString(I);

        System.out.println(pcOut);
        System.out.println(iOut);
    }

    public void printDisplay(){
        for(int y = 0; y < 32; y++){
            StringBuilder line = new StringBuilder();
            for(int x = 0; x < 64; x++){
                    char pixel = gfx[x + (y * 64)];
                    pixel = (short)pixel == 0 ? '░' : '█';
                line.append(pixel).append(pixel);
            }
            System.out.println(line);
        }
        System.out.println();
    }

    public static void main(String[] args){
        Chip8 chip8 = new Chip8();
        //chip8.printMem();
        //chip8.printRegisters();
        //chip8.printDisplay();
        chip8.loadGame("src/main/java/naren/ragu/chip8emujavafx/charTest.rom");

        for(int i = 0; i < 5; i++){
            chip8.emulateCycle();
            chip8.printDisplay();
        }

    }


}