#!/usr/bin/env python3
"""
Test script to demonstrate the XML Schema API endpoint
"""
import requests
import json

def test_schema_api():
    base_url = "http://localhost:8080"
    
    print("Testing XML Schema API...")
    print("=" * 50)
    
    try:
        # Test health endpoint
        print("1. Testing health endpoint...")
        response = requests.get(f"{base_url}/api/schema/health")
        print(f"   Status: {response.status_code}")
        print(f"   Response: {response.json()}")
        print()
        
        # Test default schema endpoint
        print("2. Testing default schema endpoint...")
        response = requests.get(f"{base_url}/api/schema/default")
        print(f"   Status: {response.status_code}")
        
        if response.status_code == 200:
            schema_data = response.json()
            print(f"   Success: {schema_data['success']}")
            if schema_data['success']:
                data = schema_data['data']
                print(f"   Target Namespace: {data['targetNamespace']}")
                print(f"   Element Form Default: {data['elementFormDefault']}")
                print(f"   Number of Elements: {len(data['elements'])}")
                
                # Show first element details
                if data['elements']:
                    first_element = data['elements'][0]
                    print(f"   First Element: {first_element['name']}")
                    print(f"   Element Type: {first_element['type']}")
                    print(f"   Min Occurs: {first_element['minOccurs']}")
                    print(f"   Max Occurs: {first_element['maxOccurs']}")
                    
                    if first_element.get('children'):
                        print(f"   Children: {len(first_element['children'])}")
                        for child in first_element['children'][:3]:  # Show first 3 children
                            print(f"     - {child['name']} ({child['type']})")
                    
                    if first_element.get('attributes'):
                        print(f"   Attributes: {len(first_element['attributes'])}")
                        for attr in first_element['attributes'][:3]:  # Show first 3 attributes
                            required = attr.get('use') == 'required'
                            print(f"     - {attr['name']} ({attr['type']}, {attr['use']}, required={required})")
                
                print("\n   Full JSON Response:")
                print(json.dumps(schema_data, indent=2))
            else:
                print(f"   Error: {schema_data.get('error', 'Unknown error')}")
        else:
            print(f"   Error: HTTP {response.status_code}")
            print(f"   Response: {response.text}")
            
    except requests.exceptions.ConnectionError:
        print("   Error: Could not connect to the server.")
        print("   Make sure the backend is running on http://localhost:8080")
    except Exception as e:
        print(f"   Error: {e}")

if __name__ == "__main__":
    test_schema_api()
