#!/bin/bash
# Metadata Generator for Chunk Files
# Usage: ./generate_metadata.sh <base_filename> <number_of_chunks>
# Example: ./generate_metadata.sh myfile.txt 3

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <base_filename> <number_of_chunks>"
    echo "Example: $0 myfile.txt 3"
    exit 1
fi

BASE_FILE="$1"
NUM_CHUNKS="$2"
WORKSPACE="$HOME/workspace/2"
META_FILE="$WORKSPACE/${BASE_FILE}.meta.json"

# Check if workspace exists
if [ ! -d "$WORKSPACE" ]; then
    echo "Error: Workspace directory not found: $WORKSPACE"
    exit 1
fi

# Start JSON
echo "{" > "$META_FILE"
echo "  \"totalChunks\": $NUM_CHUNKS," >> "$META_FILE"
echo "  \"chunks\": [" >> "$META_FILE"

# Generate chunk entries
for i in $(seq 1 $NUM_CHUNKS); do
    CHUNK_NAME="${BASE_FILE}.chunk${i}"
    CHUNK_PATH="$WORKSPACE/$CHUNK_NAME"
    
    # Check if chunk exists
    if [ ! -f "$CHUNK_PATH" ]; then
        echo "Warning: Chunk file not found: $CHUNK_PATH"
        CRC="00000000"
    else
        # Calculate CRC32 using Python
        CRC=$(python3 -c "
import zlib
with open('$CHUNK_PATH', 'rb') as f:
    data = f.read()
    crc = zlib.crc32(data) & 0xffffffff
    print(format(crc, '08X'))
")
    fi
    
    # Add chunk entry
    echo "    {" >> "$META_FILE"
    echo "      \"name\": \"$CHUNK_NAME\"," >> "$META_FILE"
    echo "      \"crc32\": \"$CRC\"" >> "$META_FILE"
    
    # Add comma if not last chunk
    if [ $i -lt $NUM_CHUNKS ]; then
        echo "    }," >> "$META_FILE"
    else
        echo "    }" >> "$META_FILE"
    fi
done

# Close chunks array and add metadata
echo "  ]," >> "$META_FILE"
echo "  \"owner\": \"2\"," >> "$META_FILE"
echo "  \"allowedUsers\": []" >> "$META_FILE"
echo "}" >> "$META_FILE"

echo "✅ Metadata file created: $META_FILE"
echo ""
echo "Contents:"
cat "$META_FILE"
