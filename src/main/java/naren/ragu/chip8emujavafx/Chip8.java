package naren.ragu.chip8emujavafx;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Chip8 implements Serializable {

    Random random;
    Dictionary<String, Boolean> quirks = new Hashtable<>();
    String romPath;
    boolean emulate = true;
    boolean beep = false;

    boolean drawFlag;

    char opcode;

    char[] memory;

    char[] V; // register file

    char I; // index counter
    char pc; // program counter

    // 0x000-0x1FF - Chip 8 interpreter (contains font set in emu)
    // 0x050-0x0A0 - Used for the built-in 4x5 pixel font set (0-F)
    // 0x200-0xFFF - Program ROM and work RAM

    byte[] gfx;

    char delay_timer;
    char sound_timer;

    char[] stack;
    char sp;

    char[] key;

    char[] fontset;


    public Chip8(){
        // Initialize quirks dictionary
        quirks.put("shift", false);
        quirks.put("memoryIncrementByX", false);
        quirks.put("memoryLeaveIUnchanged", false);
        quirks.put("jump", false);
        quirks.put("wrap", false);
        quirks.put("vfreset", false);


        random = new Random();
        memory = new char[4096];
        V = new char[16];
        gfx = new byte[64 * 32];
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
        //gfx = new byte[64 * 32];
        Arrays.fill(gfx, (byte) 0);
        // Clear stack
        //stack = new char[16];
        Arrays.fill(stack, (char) 0);
        // Clear registers V0-VF
        //V = new char[16];
        Arrays.fill(V, (char) 0);
        // Clear memory
        //memory = new char[4096];
        Arrays.fill(memory, (char) 0);

        // Load fontset
        System.arraycopy(fontset, 0, memory, 80, 80);

        // Reset timers
        delay_timer = 0;
        sound_timer = 0;

        drawFlag = true;
        beep = false;
        //memory[0x1FF] = 0x1;
    }

    void emulateCycle(){

        if(!emulate){
            beep = false;
            return;
        }

        // Fetch Opcode
        char hi = memory[pc++];
        char lo = memory[pc++];
        opcode = (char) ((hi << 8) | lo);

        char x = (char) (hi & 0xF);
        char y = (char) (lo >>> 4);
        char n = (char) (lo & 0xF);
        char nnn = (char) (opcode & 0xFFF);
        // Decode Opcode

        // Get instruction
        switch (opcode & 0xF000) {
            case 0x0000:
                switch (nnn) {
                    case 0x0E0: // 0x00E0 : Clear screen
                        Arrays.fill(gfx, (byte) 0);
                        drawFlag = true;
                        break;
                    case 0x0EE: // 0x00EE : Return from subroutine
                        pc = stack[--sp];
                        drawFlag = true;
                        break;
                    case(0x000):
                        emulate = false;
                        break;
                    default:
                        System.out.println("Invalid 0 Opcode " + Integer.toHexString(opcode & 0xFFFF));
                        break;
                }
                break;
            case 0x1000: // 0x1XXX : Jump to address at XXX
                pc = nnn;
                break;
            case 0x2000: // 0x2XXX : Call subroutine at XXX
                stack[sp++] = pc;
                pc = nnn;
                break;
            case 0x3000: // 0x3XNN : Skip next instruction if RX == NN
                if (V[x] == lo) {
                    incPC();
                }
                break;
            case 0x4000: // 0x4XNN : Skip next instruction if RX != NN
                if (V[x] != lo) {
                    incPC();
                }
                break;
            case 0x5000: // 0x5XY0 : Skip next instruction if RX == RY
                // instruction only valid when n == 0
                if (V[x] == V[y]) {
                    incPC();
                }
                break;
            case 0x6000: // 0x6XNN : Set RX to NN
                V[x] = lo;
                break;
            case 0x7000:  // 0x7XNN : Add NN to RX
                V[x] = (char) ((V[x] + lo) & 0xFF);
                break;
            case 0x8000: { // Bitwise operations
                switch (n) {
                    case 0x0: // 8XY0 : set RX to RY
                        V[x] = V[y];
                        break;
                    case 0x1: // 8XY1 : set RX to bitwise RX or RY
                        V[x] |= V[y];
                        if(quirks.get("vfreset")) V[0xF] = 0;
                        break;
                    case 0x2: // 8XY2 : set RX to bitwise RX and RY
                        if(quirks.get("vfreset")) V[0xF] = 0;
                        V[x] &= V[y];
                        break;
                    case 0x3: // 8XY3 : set RX to bitwise RX xor RY
                        if(quirks.get("vfreset")) V[0xF] = 0;
                        V[x] ^= V[y];
                        break;
                    case 0x4: { // 8XY4 : adds RY to RX, RF is set to 1 if overflowed, 0 if not
                        char sum = (char) (V[x] + V[y]);
                        V[x] = (char) (sum & 0xFF);
                        V[0xF] = (char) (sum >>> 8);
                        break;
                    }
                    case 0x5: { // 8XY5 : sets RX to RX - RY, RF is 0 when underflow, 1 if not (when RX >= RY)
                        char noBorrow = (char) (V[x] >= V[y] ? 1 : 0);
                        V[x] = (char) ((V[x] - V[y]) & 0xFF);
                        V[0xF] = noBorrow;
                        break;
                    }
                    case 0x6: { // 8XY6 : store least significant bit of RY in RF, set RX to RY shifted right by 1
                        Boolean shift = quirks.get("shift");
                        if (shift == false) {
                            char lsb = (char) (V[y] & 0x1);
                            V[x] = (char) (V[y] >>> 1);
                            V[0xF] = lsb;
                        }
                        else {
                            char lsb = (char) (V[x] & 0x1);
                            V[x] = (char) (V[x] >>> 1);
                            V[0xF] = lsb;
                        }
                        break;
                    }
                    case 0x7: { // 8XY7 : sets RX to RY - RX, RF is 0 when underflow, 1 if not (when RY >= RX)
                        char noBorrow = (char) (V[y] >= V[x] ? 1 : 0);
                        V[x]   = (char) ((V[y] - V[x]) & 0xFF);
                        V[0xF] = noBorrow;
                        break;
                    }
                    case 0xE: { // 8XYE : store most significant bit of RY in RF, shift RX to RY shifted left by 1
                        Boolean shift = quirks.get("shift");
                        if (shift == false) {
                            char msb = (char) (V[y] >>> 7);
                            V[x] = (char) ((V[y] << 1) & 0xFF);
                            V[0xF] = msb;
                        }
                        else{
                            char msb = (char) (V[x] >>> 7);
                            V[x] = (char) ((V[x] << 1) & 0xFF);
                            V[0xF] = msb;
                        }
                        break;
                    }
                    default:
                        System.out.println("Invalid 8 Opcode " + Integer.toHexString(opcode));
                        break;
                }
                break;
            }
            case 0x9000: // 0x9XY0 : Skip next instruction if RX != RY
                // instruction only valid when n == 0
                if (V[x] != V[y]) {
                    incPC();
                }
                break;
            case 0xA000: // ANNN : set I to address NNN
                I = nnn;
                break;
            case 0xB000: // BNNN : jump to address NNN + R0
                Boolean jumpQuirk = quirks.get("jump");
                if(jumpQuirk){
                    pc = (char) (V[x] + nnn);
                }
                else{
                    pc = (char) (V[0] + nnn);
                }
                break;
            case 0xC000: // CXNN : Set RX to result of bitwise NN and rand(0 to 255)
                V[x] = (char) (random.nextInt(256) & lo);
                break;
            case 0xD000: { // DXYN : draw sprite at coordinate XY with width 8 and height N
                char _x = V[x];
                char _y = V[y];
                char pixel;

                V[0xF]   = 0;
                drawFlag = true;

                for (int row = 0; row < n; ++row) {
                    char ny = (char) (_y + row);
                    if (ny >= 32 && !quirks.get("wrap")) break;
                    ny = (char) (ny % 32);

                    pixel = memory[I + row];

                    for (int col = 0; col < 8; col++) {
                        char nx = (char) (_x + col);
                        if (nx >= 64 && !quirks.get("wrap")) break;
                        nx = (char) (nx % 64);

                        if ((pixel & (0x80 >> col)) != 0) {
                            char pos = (char) (ny * 64 + nx);

                            if (gfx[pos] == 1) V[0xF] = 1;

                            gfx[pos] ^= 1;
                        }
                    }
                }
                break;
            }
            case 0xE000: // key operations
                switch (lo) {
                    case 0x9E: // EX9E : Skip next instruction if key == RX
                        if(key[V[x] & 0xF] != 0) {
                            incPC();
                        }
                        break;
                    case 0xA1: // EXA1 : Skip next instruction if key != RX
                        if(key[V[x] & 0xF] == 0) {
                            incPC();
                        }
                        break;
                    default:
                        System.out.println("Invalid E Opcode " + Integer.toHexString(opcode));
                        break;
                }
                break;
            case 0xF000:
                switch (lo) {
                    case 0x07: // FX07 : Store delay timer in RX
                        V[x] = delay_timer;
                        break;
                    case 0x0A: // FX0A : A key release is awaited, and then stored in RX
                        for (int i = 0; i < 16; i++) {
                            if (key[i] != 0) {
                                V[x] = (char) i;
                                return;
                            }
                        }
                        dontIncPC();
                        break;
                    case 0x15: // FX15 : Set delay timer to RX
                        delay_timer = V[x];
                        //System.out.println("Set delay timer to " + (int)delay_timer);
                        break;
                    case 0x18: // FX18 : Set sound timer to RX

                        // it shouldn't by multiplied by 2 but this is the only way that sound works properly until i fix this
                        // TODO : FIX THIS
                        sound_timer = (char)(V[x]);
                        break;
                    case 0x1E: // FX1E : Adds RX to I (RX flag is not changed)
                        I += V[x];
                        break;
                    case 0x29: // FX29 : Sets I to the location of the sprite for the character in RX
                        I = (char) (5 * (V[x] & 0xF) + 0x50);
                        drawFlag = true;
                        break;
                    case 0x33: // FX33 : Stores binary coded decimal representation of RX at I, I+1, and I+2
                        memory[I] = (char) (V[x] / 100);
                        memory[I + 1] = (char) ((V[x] % 100) / 10);
                        memory[I + 2] = (char) ((V[x] % 100) % 10);
                        break;
                    case 0x55: { // FX55 : Stores from R0 - RX in memory, starting at address I. offset += 1 for each value (I itself doesn't change)
                        System.arraycopy(V, 0, memory, I, x + 1);
                        Boolean loadStoreQuirkX = quirks.get("memoryIncrementByX");
                        Boolean loadStoreQuirkI = quirks.get("memoryLeaveIUnchanged");

                        if(!loadStoreQuirkI){
                            if (loadStoreQuirkX) {
                                I += x;
                            } else {
                                I += (char) (x + 1);
                            }
                        }

                        break;
                    }
                    case 0x65: { // FX65 : Loads into R0 - RX from memory, starting at address I. offset += 1 for each value (I itself doesn't change)
                        System.arraycopy(memory, I, V, 0, x + 1);
                        Boolean loadStoreQuirkX = quirks.get("memoryIncrementByX");
                        Boolean loadStoreQuirkI = quirks.get("memoryLeaveIUnchanged");

                        if(!loadStoreQuirkI){
                            if (loadStoreQuirkX) {
                                I += x;
                            } else {
                                I += (char) (x + 1);
                            }
                        }
                        break;
                    }
                    default:
                        System.out.println("Invalid F Opcode " + Integer.toHexString(opcode));
                        break;
                }
                break;
            default:
                System.out.println("Invalid Opcode " + Integer.toHexString(opcode));
                break;
        }
    }

    // Update timers
    void updateTimers(){

        if (delay_timer > 0) {
            --delay_timer;
        }

        if (sound_timer > 0) {
            beep = true;
            --sound_timer;
            //System.out.println("beep");
        }
        else{
            beep = false;
        }
    }

    void loadGame(String name){
        try {
            Path path = Paths.get(name);
            romPath = name;
            byte[] contents = Files.readAllBytes(path);
            for (int i = 0; i < contents.length; i++){
                char data = (char)(0xFF & contents[i]);
                memory[i + 0x200] = data;
            }
        } catch (IOException e) {
            System.out.println("Game File Not Found: " + name);
        }
    }

    void loadGame(InputStream is){
        try {
            romPath = "demo";
            byte[] contents = is.readAllBytes();
            for (int i = 0; i < contents.length; i++){
                char data = (char)(0xFF & contents[i]);
                memory[i + 0x200] = data;
            }
        } catch (IOException e) {
            System.out.println("InputStream not found! Cannot load ROM.");
        }
    }

    public byte[] getGfx(){
        return gfx;
    }

    public void setKeys(char[] keys){
        this.key = keys;
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
                    char pixel = (char) gfx[x + (y * 64)];
                    pixel = (short)pixel == 0 ? '░' : '█';
                line.append(pixel).append(pixel);
            }
            System.out.println(line);
        }
        System.out.println();
    }
}

