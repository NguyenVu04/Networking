package torrent.network.client.peerconnection.uploader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringJoiner;

import torrent.network.client.torrentbuilder.TorrentBuilder;
import torrent.network.client.torrentexception.ExceptionHandler;

public class ServedFileEntity {
    private File file;
    private boolean[] pieces;
    private boolean done;

    public ServedFileEntity(String file, int numberOfPieces) throws Exception {
        this.file = new File(file);

        if (this.file.exists() && this.file.isFile()) {

            this.pieces = new boolean[numberOfPieces];
            Arrays.fill(this.pieces, true);
            this.done = true;

        } else if (this.file.exists()) {
            File[] files = this.file.listFiles();
            this.done = false;

            this.pieces = new boolean[numberOfPieces];
            Arrays.fill(this.pieces, false);

            for (File filePiece : files) {
                String[] pieceName = filePiece.getName().split("\\.");

                if (pieceName.length >= 3) {
                    StringJoiner joiner = new StringJoiner(".");
                    for (int i = 0; i < pieceName.length - 1; i++) {
                        joiner.add(pieceName[i]);
                    }
                    String name = joiner.toString();

                    if (name.equals(this.file.getName())) {
                        try {
                            int index = Integer.parseInt(pieceName[pieceName.length - 1]);
                            this.pieces[index] = true;
                        } catch (Exception e) {
                            ExceptionHandler.handleException(e);
                        }
                    }
                }
            }
            for (boolean piece : this.pieces) {
                if (!piece) {
                    this.done = false;
                    return;
                }
            }

            this.mergeFiles();
        } else {
            if (!this.file.mkdirs())
                throw new IOException("Failed to create directory: " + this.file.getAbsolutePath());

            this.pieces = new boolean[numberOfPieces];
            Arrays.fill(this.pieces, false);
            this.done = false;
        }
    }

    public File getFile() {
        return this.file;
    }

    public boolean isDone() {
        return this.done;
    }

    public boolean hasPiece(int index) {
        if (index < 0 || index >= this.pieces.length)
            return false;
        return this.pieces[index];
    }

    public byte[] getPiece(int index) throws Exception {
        if (index < 0 || index >= this.pieces.length)
            throw new IOException("Index out of range: " + index);

        if (this.done) {
            try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(this.file))) {
                input.skip(index * TorrentBuilder.pieceSize);
                byte[] piece = input.readNBytes(TorrentBuilder.pieceSize);

               return piece;
            } catch (Exception e) {
                this.done = false;
                this.pieces[index] = false;
                throw new IOException(e);
            }
        }

        try (BufferedInputStream input = new BufferedInputStream(
                new FileInputStream(
                        this.file.getAbsolutePath() + File.separator + this.file.getName() + "." + index))) {

            byte[] piece = input.readAllBytes();

            if (piece.length > TorrentBuilder.pieceSize) {
                throw new IOException("Invalid piece length: " + piece.length);
            }

            return piece;
        } catch (Exception e) {
            this.pieces[index] = false;
            this.done = false;
            throw new IOException(e);
        }
    }

    public byte[] getBitfield() throws Exception {
        byte[] bitfield = new byte[this.pieces.length];
        for (int i = 0; i < this.pieces.length; i++) {
            bitfield[i] = (byte) (this.pieces[i] ? 1 : 0);
        }
        return bitfield;
    }

    public void savePiece(int index, byte[] piece) throws Exception {
        if (index < 0 || index >= this.pieces.length)
            throw new IOException("Index out of range: " + index);

        if (this.done)
            return;

        try (BufferedOutputStream output = new BufferedOutputStream(
                new FileOutputStream(this.file.getAbsolutePath() +
                        File.separator + this.file.getName() +
                        "." + index))) {

            output.write(piece);
            output.flush();

            this.pieces[index] = true;

            for (int i = 0; i < this.pieces.length; i++) {
                if (!this.pieces[i]) {
                    this.done = false;
                    return;
                }
            }
        }

        this.mergeFiles();
    }

    private void mergeFiles() {
        String newPath = this.file.getAbsolutePath() + ".temp";
        for (int i = 0; i < this.pieces.length; i++) {
            try (BufferedOutputStream out = new BufferedOutputStream(
                    new FileOutputStream(newPath, true))) {
                byte[] buffer = this.getPiece(i);
                out.write(buffer);
                out.flush();
            } catch (Exception e) {
                ExceptionHandler.handleException(e);
            }
        }

        File[] files = this.file.listFiles();

        for (File file : files) {
            file.delete();
        }

        this.file.delete();

        this.done = true;

        File newFile = new File(newPath);
        newFile.renameTo(this.file);
    }
}
